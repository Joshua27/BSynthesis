
MACHINE beverage_vending_machine
ABSTRACT_VARIABLES
  coins,
  soda
/* PROMOTED OPERATIONS
  three_for_two,
  insert_coin,
  get_soda */
INVARIANT
    coins : NATURAL
  & soda : NATURAL
INITIALISATION
    BEGIN
         coins := 5
      ||
         soda := 4
    END
OPERATIONS
  insert_coin = 
    PRE coins < 10 THEN
         coins := 1 + coins
    END;

  insert_soda = 
    PRE soda < 15 THEN
         soda := soda + 1
    END;

  get_soda = 
    PRE 
        coins >= 0 
      & soda > 0
    THEN
         coins := -1 + coins
      ||
         soda := soda + -1
    END;

  three_for_two = 
    PRE 
        coins > 1
      & soda > 2
    THEN
         coins := coins - 1
      ||
         soda := soda - 3
    END
END
