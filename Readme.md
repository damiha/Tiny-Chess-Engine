# Minimax Chess Engine

What is implemented:

- chess with all standard rules (including en passant, draw by 3-fold repetition, 50-move rule, draw by insufficient material)
- minimax with alpha-beta pruning and move sorting
- quiescence search
- iterative deepening search with a bit of memoization to speed up the pruning (the iterative deepening framework is a simple realization of time management)
- opening book
- loading in games from PGN (portable game notation) and FEN

What does the static evaluation consider:

- center control (pawns and minor pieces are encouraged, king and rook/queen are discouraged)
- piece mobility (fixed bonus per legal move)
- king safety (pawn structure around the king and castling right)
- past pawns (the distance the past pawns is travelled is important)
- a "value difference factor" (the engine trades down when ahead in material and avoids trades when it's at a material disadvantage)
- a seperate static evaluation function for common endgames (king and rook vs. king, king and bishops vs. king etc.)


How fast is it:

- legal move generation makes it quiet slow. We get around 50k to 80k calls to the static evaluation function per second
- we can search 4 half moves (+ 4 half moves for the quiescence search) in 15 seconds


How strong is it:

- with 15 seconds max. response times, it confidently beats 1500 and wins/loses against 1600 and 1700 bots half of the time
(so I guess its somewhere around 1650)
- per game there are generally one or two situations where it blunders because of the horizon effect (apart from those, it plays rather decent chess, accuracy by stockfish around 75%)

What is missing:

- engine should think when it is the opponent's turn
- a fined tuned (and data driven) evaluation function
- general optimizations (bitboards, killer heuristic etc)
- exporting games to PGN to analyze them with e.g. Stockfish
- connection to stockfish to get a better sense of its strength (current sample size is quite small)
