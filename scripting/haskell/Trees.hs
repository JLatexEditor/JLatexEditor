
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

depth_lr :: String -> Term -> Int
depth_lr n term
  | n == ""        = 0
  | n == name term = 0
  | otherwise      = 
      minimum (999 : map (\(i,t) -> depth_lr n t + (2*i - 1 - (length $ subterms term))) (zip [1..] (subterms term)))

occurs n term 
  | n == name term = True
  | otherwise      = or $ map (occurs n) (subterms term)   

nested n m term 
  | n == ""        = True
  | n == name term = occurs m term
  | m == name term = occurs n term
  | otherwise      = or $ map (nested n m) (subterms term)   

data Direction = Dec | Eq | Inc

directions :: [Int] -> [Direction]
directions (a:b:xs)
  | a < b = Inc : directions (b:xs)
  | a > b = Dec : directions (b:xs)
  | otherwise = Eq : directions (b:xs)
directions _ = []

up_down :: Term -> [String] -> [(String,Direction,String,Direction,String)]
up_down term nodes =
  let depths = map (\n -> depth n term) nodes
      zip5 (a:as) (b:bs) (c:cs) (d:ds) (e:es) = (a,b,c,d,e) : zip5 as bs cs ds es
      zip5 _ _ _ _ _ = [] 
  in zip5 
      ("" : nodes)
      (directions $ (-1):depths)
      nodes
      (directions $ depths ++ [-1])
      (tail nodes ++ [head nodes])

north_east n = "($(" ++ n ++ ".north east) + (.7mm,-.7mm)$)"
north_west n = "($(" ++ n ++ ".north west) + (-.7mm,-.7mm)$)"
south_east n = "($(" ++ n ++ ".south east) + (.7mm,.7mm)$)"
south_west n = "($(" ++ n ++ ".south west) + (-.7mm,.7mm)$)"
south n = "($(" ++ n ++ ".south) + (0mm,-1mm)$)"
north n = "($(" ++ n ++ ".north) + (0mm,1mm)$)"
west n = "($(" ++ n ++ ".west) + (-1mm,0mm)$)"
east n = "($(" ++ n ++ ".east) + (1mm,0mm)$)"
on_way_left p n = "($(" ++ p ++ ") !.15! 270:(" ++ n ++ ") !.4! (" ++ n ++ ")$)"
on_way_right p n = "($(" ++ p ++ ") !.15! 90:(" ++ n ++ ") !.4! (" ++ n ++ ")$)"

renderHighlight :: Term -> Highlight -> String
renderHighlight tree highlight =
  let root = head $ nodes highlight
      lst = last $ nodes highlight
  in
  "\\draw [rounded corners=1.5mm] " ++ style highlight ++ " " ++ 
  -- root
  (if depth_lr lst tree <= depth_lr root tree 
   then south_east root ++ " -- " 
   else east root ++ " -- ") ++
  north_east root ++ " -- " ++ north root ++ " -- " ++
  -- remaining path
  renderHighlight_ tree (up_down tree $ nodes highlight) 

renderHighlight_ tree [] = "cycle;"
renderHighlight_ tree ((pn,d1,n,d2,nn):ns) =
  (case d1 of
     Inc ->  (if depth_lr pn tree >= depth_lr n tree 
               then north_west n ++ " -- " 
               else west n ++ " -- ") ++
             case d2 of
               Inc -> if depth_lr nn tree >= depth_lr n tree 
                      then south_west n ++ " -- "
                      else if depth_lr pn tree >= depth_lr n tree 
                           then west n ++ " -- "
                           else ""
               Eq   -> south_west n ++ " -- " ++ south n ++ " -- " ++ south_east n ++ " -- "
               Dec   -> south_west n ++ " -- " ++ south n ++ " -- " ++ south_east n ++ " -- " ++ 
                        if depth_lr n tree >= depth_lr nn tree 
                        then north_east n ++ " -- "
                        else ""
             ++ on_way_left n nn ++ " -- " ++ on_way_right nn n ++ " -- "
     Eq  ->  south_west n ++ " -- " ++
             case d2 of
               Inc -> ""
               Eq   -> south n ++ " -- " ++ south_east n ++ " -- "
               Dec   -> south n ++ " -- " ++ south_east n ++ " -- " ++ north_east n ++ " -- "
             ++ on_way_left n nn ++ " -- " ++ on_way_right nn n ++ " -- "
     Dec ->  case d2 of
               Inc -> ""
               Eq  -> south_east n ++ " -- "
               Dec -> if depth_lr pn tree <= depth_lr n tree 
                      then south_east n ++ " -- "
                      else east n ++ " -- " ++
                           if depth_lr n tree >= depth_lr nn tree 
                           then north_east n ++ " -- "
                           else ""
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
