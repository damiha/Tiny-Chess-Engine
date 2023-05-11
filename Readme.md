# Minimax Chess Engine

## a minimalilst chess engine in Java written in less than 24 hours

What is implemented:

- A GUI
- Minimax with alpha-beta pruning
- Move sorting
- A simple static evaluation that takes into account material, piece position and castling/king safety

How fast is it?
- approx. 800k evaluations per second on a standard laptop (Acer Swift 3)
- Typical search depths 5 and 6 return within a minute

What is missing:
Some more 'exotic' chess rules:
- en passant
- choosing the piece to promote to (currently auto-promote to queen)
- no castling into check/ no castling out of check
- Quiescence search
