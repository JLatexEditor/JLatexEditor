
module Trees where

import IO hiding (try)
import System.Environment
import System.Process

import Prelude
import qualified Data.Set as Set
import qualified Data.Map as Map

import Text.ParserCombinators.Parsec as P
import qualified Text.ParserCombinators.Parsec.Token as T
import Text.ParserCombinators.Parsec.Language( javaStyle, emptyDef )
import Text.ParserCombinators.Parsec.Error hiding (Message)

import Debug.Trace

data Term = Term { 
    root :: String
  , name :: String
  , subterms :: [Term]
  , propBeforeChild :: String
  , propAfterChild :: String
  , propNode :: String
} deriving (Eq, Show)

data Highlight = Highlight {
    nodes :: [String]
  , style :: String
} deriving (Eq, Show)

main :: IO ()
main = do args <- getArgs
          tree $ args !! 0

tree string = 
    case Trees.parse string of
      Left errors -> hPutStrLn stderr $ show errors
      Right (term,highlights) -> do 
        hPutStrLn stdout $ renderTerm term
        hPutStr stdout $ "\\begin{pgfonlayer}{background}"
        hPutStrLn stdout $ concat $ map (\h -> 
          -- "\n  " ++ intersections (nodes h) ++
          "\n  " ++ renderHighlight term h) highlights
        hPutStrLn stdout $ "\\end{pgfonlayer}"

renderTerm :: Term -> String
renderTerm term = "\\" ++ renderTerm_ term ++ ";"

renderTerm_ term =
  "node" ++ propNode term ++ " (" ++ name term ++ ") {" ++ root term ++ "}" ++
  (replace "\n" "\n  " $ 
    concat $ map (\child -> "\n  " ++ propBeforeChild term ++ "child" ++ propAfterChild term ++ 
                            " { " ++ renderTerm_ child ++ "\n}") $ subterms term)

depth :: String -> Term -> Int
depth n term
  | n == ""        = -1
  | n == name term = 0
  | otherwise      = 1 + minimum (999 : map (depth n) (subterms term))

occurs n term 
  | n == name term = True
  | otherwise      = or $ map (occurs n) (subterms term)   

nested n m term 
  | n == ""        = True
  | n == name term = occurs m term
  | m == name term = occurs n term
  | otherwise      = or $ map (nested n m) (subterms term)   

{-
hPath n = "h_" ++ n
vPath n = "v_" ++ n
iName n m = "i_" ++ n ++ "_" ++ m

intersections (n:m:nodes) =
  let move p x y =  "($(" ++ p ++ ")+(" ++ show x ++ "cm," ++ show y ++ "cm)$)"
      hpath n = "\\path [draw=none,name path=" ++ hPath n ++ "] " ++ move n (-10) 0 ++ " -- " ++ move n 10 0 ++ ";\n"
      vpath n = "\\path [draw=none,name path=" ++ vPath n ++ "] " ++ move n 0 (-10) ++ " -- " ++ move n 0 10 ++ ";\n"
      intersect n m = "\\path [name intersections={of=" ++ hPath n ++ " and " ++ vPath m ++ "}];\n" ++
                      "\\coordinate ("++ iName n m ++ ") at (intersection-1);\n"
  in hpath n ++ vpath n ++ hpath m ++ vpath m ++ intersect n m ++ intersect m n ++ intersections (m:nodes)
intersections _ = ""
-}

renderHighlight :: Term -> Highlight -> String
renderHighlight tree highlight =
  "\\draw [rounded corners=1.5mm] " ++ style highlight ++ " " ++ renderHighlight_ tree "" (nodes highlight) 

north_east y n = "($(" ++ n ++ ".north east) + (1mm," ++ show y ++ "mm)$)"
north_west y n = "($(" ++ n ++ ".north west) + (-1mm," ++ show y ++ "mm)$)"
south_east n = "($(" ++ n ++ ".south east) + (1mm,0mm)$)"
south_west n = "($(" ++ n ++ ".south west) + (-1mm,0mm)$)"
south n = "($(" ++ n ++ ".south) + (0mm,-1mm)$)"
north n = "($(" ++ n ++ ".north) + (0mm,1mm)$)"

renderHighlight_ tree p [] = south p ++ " -- " ++ south_east p ++ " -- " ++ north_east (-1) p ++ " -- cycle;"
renderHighlight_ tree p (n:ns) =
  let nesting = nested p n tree
  in
  -- root node
  (if p == "" 
    then south_east n ++ " -- " ++ north_east (-1) n ++ " -- " ++ north n ++ " -- " 
    else "")
  ++
  -- previous node deeper or equal depth
  (if depth p tree >= depth n tree || not (nested p n tree)
    then south p ++ " -- " ++ south_east p ++ " -- " 
         ++ (if depth p tree > depth n tree then north_east (-1) p ++ " -- " else "")
         ++ (if not (nested p n tree) then south_west n ++ " -- " else "") 
    else
    (if depth p tree < depth n tree
      then north_west (-1) n ++ " -- " ++ south_west n ++ " -- "
      else south n ++ " -- "))
  ++ renderHighlight_ tree n ns

--------------------------------------------------------------------------------
-- Parsing the input language
--------------------------------------------------------------------------------

parse input = P.parse parseInput "Term with highlights" input

escape = id

-- Replace in Strings
replace :: String -> String -> String -> String
replace s r [] = []
replace s r string
  | s == take (length s) string = r ++ (replace s r $ drop (length s) string)
  | otherwise                   = (head string) : (replace s r $ tail string)

lexer :: T.TokenParser ()
lexer  = T.makeTokenParser emptyDef -- (javaStyle { T.reservedOpNames = [], T.reservedNames = [] })

delimiters    = [' ',',','.',':',';','(',')','{','}','[',']','=','\n']
whiteSpace    = T.whiteSpace lexer
lexeme        = T.lexeme lexer
symbol        = T.symbol lexer
natural       = T.natural lexer
parens        = T.parens lexer
semi          = T.semi lexer
comma         = T.comma lexer
commaSep      = T.commaSep lexer
colon         = T.colon lexer
constant c    = lexeme (P.string c)
stringLiteral = T.stringLiteral lexer

identifier :: Parser String
identifier = stringLiteral <|> escapedIdentifier <|> do
  string <- lexeme $ many1 $ noneOf delimiters
  return $ escape string

escapedIdentifier :: Parser String
escapedIdentifier = do
  constant "{"
  strings <- many ((many1 $ noneOf "{}") <|> (do s <- escapedIdentifier; return $ "{" ++ s ++ "}"))
  constant "}"
  return $ concat strings

-------------------------------------------------------------------------------
-- Parsing
-------------------------------------------------------------------------------

getName name = foldr (\c s -> replace c "" s) name ["(",")","[","]","{","}","\\","$"]  

parseInput :: Parser  (Term,[Highlight])
parseInput = do
  whiteSpace
  term <- parseTerm
  highlights <- many parseHighlight
  return (term,highlights)
  
parseTerm :: Parser Term
parseTerm = do
  pbc <- parseOption
  pac <- parseOption
  root <- identifier
  name <- (try $ do constant "@"; n <- identifier; return n) <|> return (getName root)
  pn <- parseOption
  subterms <- try (parens $ sepBy parseTerm (constant ",")) <|> return []
  return $ Term { root = root
                , name = name
                , subterms = subterms
                , propBeforeChild = if (not $ null pac) then pbc else ""
                , propAfterChild = if (not $ null pac) then pac else pbc
                , propNode = pn }

parseOption = 
  (try $ do n <- T.brackets lexer $ lexeme $ many $ noneOf "]"
            return $ "[" ++ n ++ "]")
  <|> return ""

parseHighlight :: Parser Highlight
parseHighlight = do
  constant "highlight"
  style <- parseOption
  nodes <- parens $ sepBy identifier (constant ",")
  return $ Highlight { nodes = nodes, style = style }
