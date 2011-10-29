
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

data Direction = Up | Eq | Down

directions :: [Int] -> [Direction]
directions (a:b:xs)
  | a < b = Down : directions (b:xs)
  | a > b = Up : directions (b:xs)
  | otherwise = Eq : directions (b:xs)
directions _ = []

up_down :: Term -> [String] -> [(Direction,(String,(Direction,String)))]
up_down term nodes =
  let depths = map (\n -> depth n term) nodes
  in zip (directions $ (-1):depths) $ zip nodes $ zip (directions $ depths ++ [-1]) $ (tail nodes ++ [head nodes])

north_east n = "($(" ++ n ++ ".north east) + (.7mm,-.7mm)$)"
north_west n = "($(" ++ n ++ ".north west) + (-.7mm,-.7mm)$)"
south_east n = "($(" ++ n ++ ".south east) + (.7mm,.7mm)$)"
south_west n = "($(" ++ n ++ ".south west) + (-.7mm,.7mm)$)"
south n = "($(" ++ n ++ ".south) + (0mm,-1mm)$)"
north n = "($(" ++ n ++ ".north) + (0mm,1mm)$)"
on_way_left p n = "($(" ++ p ++ ") !.15! 270:(" ++ n ++ ") !.4! (" ++ n ++ ")$)"
on_way_right p n = "($(" ++ p ++ ") !.15! 90:(" ++ n ++ ") !.4! (" ++ n ++ ")$)"

renderHighlight :: Term -> Highlight -> String
renderHighlight tree highlight =
  let root = head $ nodes highlight
  in
  "\\draw [rounded corners=1.5mm] " ++ style highlight ++ " " ++ 
  -- root
  south_east root ++ " -- " ++ north_east root ++ " -- " ++ north root ++ " -- " ++
  -- remaining path
  renderHighlight_ tree (up_down tree $ nodes highlight) 

renderHighlight_ tree [] = "cycle;"
--south p ++ " -- " ++ south_east p ++ " -- " ++ north_east p ++ " -- cycle;"
renderHighlight_ tree ((d1,(n,(d2,nn))):ns) =
  (case d1 of
     Down -> north_west n ++ " -- " ++ south_west n ++ " -- " ++
             case d2 of
               Down -> ""
               Eq   -> south n ++ " -- " ++ south_east n ++ " -- "
               Up   -> south n ++ " -- " ++ south_east n ++ " -- " ++ north_east n ++ " -- "
             ++ on_way_left n nn ++ " -- " ++ on_way_right nn n ++ " -- "
     Eq   -> south_west n ++ " -- " ++
             case d2 of
               Down -> ""
               Eq   -> south n ++ " -- " ++ south_east n ++ " -- "
               Up   -> south n ++ " -- " ++ south_east n ++ " -- " ++ north_east n ++ " -- "
             ++ on_way_left n nn ++ " -- " ++ on_way_right nn n ++ " -- "
     Up   -> case d2 of
               Down -> ""
               Eq   -> south_east n ++ " -- "
               Up   -> south_east n ++ " -- " ++ north_east n ++ " -- "
             ++ on_way_left n nn ++ " -- " ++ on_way_right nn n ++ " -- "
  ) ++ renderHighlight_ tree ns

-- || not (nested p n tree)

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
