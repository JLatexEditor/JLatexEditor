
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

data Config = Config {
    node_sep :: Double
  , edge_sep :: Double
} deriving (Eq, Show)

defaultConfig = Config {
    node_sep = 0.5
  , edge_sep = 0.2
}

main :: IO ()
main = do args <- getArgs
          tree $ args !! 0

tree string = 
    case Trees.parse string of
      Left errors -> hPutStrLn stderr $ show errors
      Right (config,term,highlights) -> do 
        hPutStrLn stdout $ renderTerm term
        hPutStr stdout $ "\\begin{pgfonlayer}{background}"
        hPutStrLn stdout $ concat $ map (\h -> 
          "\n  " ++ renderHighlight config term h) highlights
        hPutStrLn stdout $ "\\end{pgfonlayer}"

renderTerm :: Term -> String
renderTerm term = "\\" ++ renderTerm_ [] term ++ ";"

renderTerm_ names term =
  let naming = if name term `elem` names || name term == "" then "" else " (" ++ name term ++ ")"
  in
  "node" ++ propNode term ++ naming ++ " {" ++ root term ++ "}" ++
  (replace "\n" "\n  " $ 
    concat $ map (\child -> "\n  " ++ propBeforeChild child ++ "child" ++ propAfterChild child ++ 
                            " { " ++ renderTerm_ (name term:names) child ++ "\n}") $ subterms term)

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

north_east c n = "($(" ++ n ++ ".north east) + " ++ (show $ node_sep c) ++ "*(.4mm,.4mm)$)"
north_west c n = "($(" ++ n ++ ".north west) + " ++ (show $ node_sep c) ++ "*(-.4mm,.4mm)$)"
south_east c n = "($(" ++ n ++ ".south east) + " ++ (show $ node_sep c) ++ "*(.4mm,-.4mm)$)"
south_west c n = "($(" ++ n ++ ".south west) + " ++ (show $ node_sep c) ++ "*(-.4mm,-.4mm)$)"
south c n = "($(" ++ n ++ ".south) + " ++ (show $ node_sep c) ++ "*(0mm,-1mm)$)"
north c n = "($(" ++ n ++ ".north) + " ++ (show $ node_sep c) ++ "*(0mm,1mm)$)"
west c n = "($(" ++ n ++ ".west) + " ++ (show $ node_sep c) ++ "*(-1mm,0mm)$)"
east c n = "($(" ++ n ++ ".east) + " ++ (show $ node_sep c) ++ "*(1mm,0mm)$)"
on_way_left c p n = "($(" ++ p ++ ") !" ++ (show $ edge_sep c) ++ "! 270:(" ++ n ++ ") !.4! (" ++ n ++ ")$)"
on_way_right c p n = "($(" ++ p ++ ") !" ++ (show $ edge_sep c) ++ "! 90:(" ++ n ++ ") !.4! (" ++ n ++ ")$)"

renderHighlight :: Config -> Term -> Highlight -> String
renderHighlight c tree highlight =
  let root = head $ nodes highlight
      lst = last $ nodes highlight
  in
  "\\draw [rounded corners=1.5mm] " ++ style highlight ++ " " ++ 
  -- root
  (if depth_lr lst tree <= depth_lr root tree 
   then south_east c root ++ " -- " 
   else east c root ++ " -- ") ++
  north_east c root ++ " -- " ++ north c root ++ " -- " ++
  -- remaining path
  renderHighlight_ c tree (up_down tree $ nodes highlight) 

renderHighlight_ c tree [] = "cycle;"
renderHighlight_ c tree ((pn,d1,n,d2,nn):ns) =
  (case d1 of
     Inc ->  (if depth_lr pn tree >= depth_lr n tree 
               then north_west c n ++ " -- " 
               else west c n ++ " -- ") ++
             case d2 of
               Inc -> if depth_lr nn tree >= depth_lr n tree 
                      then south_west c n ++ " -- "
                      else if depth_lr pn tree >= depth_lr n tree 
                           then west c n ++ " -- "
                           else ""
               Eq  -> south_west c n ++ " -- " ++ south c n ++ " -- " ++ south_east c n ++ " -- "
               Dec -> south_west c n ++ " -- " ++ south c n ++ " -- " ++ south_east c n ++ " -- " ++ 
                      if depth_lr n tree >= depth_lr nn tree 
                      then north_east c n ++ " -- "
                      else ""
             ++ on_way_left c n nn ++ " -- " ++ on_way_right c nn n ++ " -- "
     Eq  ->  south_west c n ++ " -- " ++
             case d2 of
               Inc -> ""
               Eq  -> south c n ++ " -- " ++ south_east c n ++ " -- "
               Dec -> south c n ++ " -- " ++ south_east c n ++ " -- " ++ 
                      if depth_lr n tree >= depth_lr nn tree 
                      then north_east c n ++ " -- "
                      else east c n ++ " -- "
             ++ on_way_left c n nn ++ " -- " ++ on_way_right c nn n ++ " -- "
     Dec ->  case d2 of
               Inc -> ""
               Eq  -> south_east c n ++ " -- "
               Dec -> if depth_lr pn tree <= depth_lr n tree 
                      then south_east c n ++ " -- " ++
                           if depth_lr n tree >= depth_lr nn tree 
                           then north_east c n ++ " -- "
                           else east c n ++ " -- "
                      else east c n ++ " -- " ++
                           if depth_lr n tree >= depth_lr nn tree 
                           then north_east c n ++ " -- "
                           else ""
             ++ on_way_left c n nn ++ " -- " ++ on_way_right c nn n ++ " -- "
  ) ++ renderHighlight_ c tree ns

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
float         = T.float lexer
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

parseInput :: Parser (Config,Term,[Highlight])
parseInput = do
  whiteSpace
  config <- parseConfig <|> return defaultConfig
  term <- parseTerm
  highlights <- many parseHighlight
  return (config,term,highlights)
  
parseTerm :: Parser Term
parseTerm = do
  pbc <- parseOption
  pac <- parseOption
  root <- identifier
  name <- (try $ do constant ":"; n <- identifier; return n) <|> return (getName root)
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

parseConfig :: Parser Config
parseConfig = do
  constant "<"
  options <- sepBy parseConfigOption (constant ",")
  constant ">"
  return $ foldr (\(n,v) c -> case n of
                                  "node_sep" -> c { node_sep = v }
                                  "edge_sep" -> c { edge_sep = v }
                                  otherwise  -> c) defaultConfig options

parseConfigOption :: Parser (String,Double)
parseConfigOption = do
  name <- identifier
  constant "="
  value <- float
  return (name,value)