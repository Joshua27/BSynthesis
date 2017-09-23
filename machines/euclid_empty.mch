
MACHINE euclidean
ABSTRACT_VARIABLES
  x,
  y
/* PROMOTED OPERATIONS
  empty */
INVARIANT
    x : NATURAL
  & y : NATURAL
INITIALISATION
    BEGIN
         x := 132
      ||
         y := 14
    END
OPERATIONS
  empty = skip
END
