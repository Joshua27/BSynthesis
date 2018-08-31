
MACHINE scheduler
SETS /* enumerated */
  PID={p1,p2,p3}
ABSTRACT_VARIABLES
  active,
  ready,
  waiting
/* PROMOTED OPERATIONS
  del,
  new,
  set_active,
  set_ready,
  active_to_waiting,
  ready_to_active */
INVARIANT
    active <: PID & ready <: PID & waiting <: PID &
    ready /\ waiting = {}
  & active /\ (ready \/ waiting) = {}
  & card(active) <= 1
  & (active = {} => ready = {})
INITIALISATION
    BEGIN
         active := {}
      ||
         ready := {}
      ||
         waiting := {}
    END
OPERATIONS
  del(p_PID) = 
    PRE 
        p_PID : waiting
    THEN
      waiting := waiting - {p_PID}
    END;
  
  new(p_PID) = 
    PRE 
        p_PID : PID
      & p_PID /: active
    THEN
      waiting := waiting \/ {p_PID}
    END;
  
  set_active(p_PID) = 
    PRE 
        p_PID : waiting
      & active = {}
    THEN
         waiting := waiting - {p_PID}
      ||
         active := {p_PID}
    END;
  
  set_ready(p_PID) = 
    PRE 
        p_PID : waiting
      & active /= {}
    THEN
      waiting,ready := waiting - {p_PID},ready \/ {p_PID}
    END;
  
  active_to_waiting = 
    PRE 
        active /= {}
      & ready = {}
    THEN
         waiting := waiting \/ active
      ||
         active := {}
    END;
  
  ready_to_active(p_PID) = 
    PRE 
        p_PID : ready
      & active /= {}
    THEN
      waiting,active,ready := waiting \/ active,{p_PID},ready - {p_PID}
    END
END

