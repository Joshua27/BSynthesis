
MACHINE beverage_vending_machine
ABSTRACT_VARIABLES
  coins,
  soda
/* PROMOTED OPERATIONS
  insert_coin,
  insert_soda */
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
  insert_soda = 
    PRE 
        soda < 15
    THEN
      soda := soda + 1
    END;
  
  insert_coin = 
    PRE 
        coins < 10
    THEN
      coins := 1 + coins
    END
END
