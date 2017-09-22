
MACHINE beverage_vending_machine
ABSTRACT_VARIABLES
  coins,
  soda
/* PROMOTED OPERATIONS
  insert_soda,
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
    PRE coins > 0 & soda > 0 THEN
	coins := coins -1 || soda := soda - 1
    END
END
