# Repair and Generation of Formal Models Using Synthesis

[![Build Status](https://travis-ci.org/Joshua27/BSynthesis.svg?branch=master)](https://travis-ci.org/Joshua27/BSynthesis)
[![Dependency Status](https://www.versioneye.com/user/projects/592ac926a8a056006137f3fb/badge.svg?style=flat-square)](https://www.versioneye.com/user/projects/592ac926a8a056006137f3fb)
[![Sonarqube Quality Gate](https://sonarqube.com/api/badges/gate?key=de.hhu.stups.bsynthesis)](https://sonarqube.com/dashboard?id=de.hhu.stups.bsynthesis)
[![Sonarqube Tech Debt](https://sonarqube.com/api/badges/measure?key=de.hhu.stups.bsynthesis&metric=sqale_debt_ratio)](https://sonarqube.com/component_measures/domain/Maintainability?id=de.hhu.stups.bsynthesis)
<!---[![Sonarqube Coverage](https://sonarqube.com/api/badges/measure?key=de.hhu.stups.bsynthesis&metric=coverage)](https://sonarqube.com/component_measures/domain/Coverage?id=de.hhu.stups.bsynthesis)-->

# Background

For safety or business critical systems a software bug can lead to extensive safety, security or financial problems.
To overcome these issues, one can use a rigorous, formal development method.
In particular, one might use a model driven approach, centering around thorough specification of the software to be developed.
This involves describing the system at various refinement levels and specifying system properties, for instance, in the form of invariants or preconditions for operations.
However, writing a formal model is a complicated and time-consuming task.
Usually, one successively refines a model with the help of provers or model checkers.
In case an error is found or if a proof fails, the model has to be adapted.
In general, finding the appropriate set of changes is non-trivial.
Relating to the formal specification language B it is conventional to think in terms of states describing explicit values of the machine variables at a time.
These states can straightforwardly be used for synthesis.

Hence, we propose partially automating the development process of formal models using synthesis:
Guided by examples of positive and negative behavior we strengthen preconditions or relax invariants appropriately.
Moreover, by collecting initial examples from the user we generate operations from scratch or adapt existing ones.
All this is done using constant user feedback, yielding an interactive assistant.

# Requirements

- Java 8 Oracle JDK
- Gradle 3.2.1 (or newer)

# Start the Application

Build the application yourself by executing `gradle run` from the root folder.

Or download the latest release (see the attached "README" file for more information).

# References

The synthesis technique in use is based on the one by Jha et.al. introduced in ["Oracle-guided component-based program synthesis"](https://people.eecs.berkeley.edu/~sseshia/pubdir/icse10-TR.pdf), Proceedings ICSE, 2010.

# Benchmarks

In the following we will briefly evaluate the performance of the synthesis backend.
We use the average time of ten independent runs for each example.
All presented times are measured in seconds.
The benchmarks were run on a system with an Intel Core I7-7700HQ CPU (2.8GHz) and 32GB of RAM.
We compare the runtime of using the exact library components that are necessary to synthesize a program as well as a default library configuration.
This default library configuration starts with as few components as possible and successively increases the size of the library if no solution can be found.
The library expansion stops if a solution has been found or a predefined threshold value has been exceeded.
In this context, we do not parallelize the default library configuration but use a single core only.
The tool finally parallelizes synthesis for different library configurations in Java.
Furthermore, we provide as many examples as necessary to find a unique solution without demanding an interaction with the user.
We used a maximum timeout of 10 minutes.
There is no standardized set of benchmarks for synthesizing classical B programs.
To that effect, some of the programs are constructed, while others refer to real life applications like a scheduler managing the states of several processes.

| Program              | Exact Library | Timeout      | Default Library | Timeout | Examples |
|----------------------|---------------|---------|-----------------|---------|----------|
|                      | (in seconds)  | (in seconds) | (in seconds)    | (in seconds) |  |
| 1                    | 0.013         | 0.5     | 0.673           | 0.5     | 2        |
| 2                    | 0.381         | 0.5     | 4.920           | 5       | 4        |
| 3                    | 0.661         | 0.5     | 12.06           | 10      | 10       |
| 4                    | 0.061         | 0.5     | 3.416           | 4       | 4        |
| 5                    | 2.569         | 2.5     | 18.37           | 4       | 4        |
| 7                    | 0.830         | 0.5     | 57.26           | 4       | 4        |
| 8                    | 9.506         | 2.5     | Timeout         | max       | 5        |
| 9                    | 10.670        | 8       | 11.32           | 6       | 6        |
| 10                   | 463.860       | 240     | Timeout         | max       | 6        |
| 11                   | 434.210       | 240     | 510.197         | 6       | 6        |
| 12                   | 0.265         | 0.5     | 46.541          | 4       | 4        |
| 13                   | 3.706         | 2.5     | 155.18          | 4       | 4        |
| 14                   | 3.748         | 2.5     | 234.73          | 8       | 8        |
| 15                   | 2.984         | 2.5     | 39.957          | 5       | 5        |
| 16                   | 18.913        | 10      | 58.26           | 10      | 10       |
| 17                   | 0.102         | 0.5     | 30.735          | 5       | 5        |
| 18                   | 9.131         | 15      | Timeout         | max       | 4        |
| 19                   | 2.525         | 2.5     | 21.36           | 3       | 3        |
| 20                   | 0.527         | 0.5     | 31.82           | 3       | 3        |
| 21                   | 1.912         | 2.5     | 11.02           | 7       | 7        |
| 22                   | 0.117         | 0.5     | 155.342         | 7       | 7        |
| 23                   | 5.246         | 2.5     | Timeout         | max       | 7        |
| 24                   | 16.173        | 10      | 97.21           | 8       | 8        |
| 25                   | 0.317         | 0.5     | 9.823           | 4       | 4        |
| 26                   | 0.092         | 0.5     | 7.640           | 0.5     | 6        |
| 27                   | 1.490         | 1.0     | 229.340         | 30.0    | 8        |
| 28                   | 0.049         | 0.5     | 0.388           | 0.5     | 5        |
| 29                   | 0.193         | 0.5     | 4.650           | 0.5     | 7        |
| 30                   | 0.126         | 0.5     | 1.730           | 0.5     | 6        |
| 31                   | 0.364         | 0.5     | 10.920          | 2.0     | 8        |
| 32                   | 0.743         | 0.5     | 6.830           | 1.5     | 8        |
| 33                   | 2.318         | 1.5     | 11.210          | 1.5     | 9        |
| 34                   | 2.923         | 2.0     | 13.301          | 5.0     | 11       |
| 35                   | 3.125         | 1.0     | 8.887           | 1.0     | 10       |


```
eval_1 =
  BEGIN
       i_1 := i_1 + 1
    ||
       i_2 := i_2
  END
```

```
eval_2 =
  PRE i_1 > 0 & i_2 > 0 THEN
       i_1 := i_1 - 1
    ||
       i_2 := i_2 - 1
  END
```

```
eval_3 =
  PRE i_1 > 1 & i_2 > 2 THEN
       i_1 := i_1 - 2
    ||
       i_2 := i_2 - 3
  END
```

```
eval_4 =
  BEGIN
       b := b
    ||
       s := s \/ {2}
    ||
       i := i * 4
  END
```

```
eval_5 =
  BEGIN
       s_1 := s_1 \/ s_2
    ||
       s_2 := s_1 - s_2
    ||
       i_1 := i_1 ** i_2
    ||
       i_2 := i_1 * i_2
  END
```

```
eval_6 =
  BEGIN
       s_1 := s_1 \/ (s_2 /\ s_3)
    ||
       s_2 := s_2
    ||
       s_3 := s_2 \/ s_3
  END
```

```
eval_7 =
  BEGIN
       s_1 := s_1
    ||
       s_2 := s_1 /\ s_2
    ||
       b_1 := bool(s_1 <: s_2)
    ||
       seq_1 := seq_1
    ||
       i_1 := i_1 * first(seq_1)
  END
```

```
eval_8 =
  BEGIN
       s_1 := s_1 - s_2
    ||
       s_2 := s_1 /\ s_2
    ||
       s_3 := s_3 \/ (s_1 \/ s_2)
    ||
       seq_1 := seq_1
    ||
       b_1 := bool(s_1 <: s_2)
    ||
       seq_2 := seq_2 ^ seq_1
  END
```


```
eval_9 =
  BEGIN
       a := IF a > 9 THEN a - -3 ELSE a + -3 END
  END
```

```
eval_10 =
  BEGIN
       s_1 := IF i_1 < 10 THEN s_1 ELSE s_1 \/ {2} END
    ||
       i_1 := IF i_1 < 10 THEN i_1 ELSE i_1 + 2 END
  END
```

```
eval_11 =
  BEGIN
       i_1 := IF i_1 > i_2 THEN i_1 - i_2 ELSE i_1 END
    ||
       i_2 := IF i_1 > i_2 THEN i_2 ELSE i_2 - i_1 END
  END
```

```
eval_12 =
  BEGIN
       s_1 := union(s_4) \/ (s_1 \/ s_2)
    ||
       s_2 := s_1 \/ s_2
    ||
       s_4 := s_4
    ||
       b_1 := bool(s_2 <: union(s_4))
  END
```

```
eval_13 =
  BEGIN
       s_1 := s_1 \/ {"test"}
    ||
       s_2 := s_1 /\ s_2
    ||
       i_2 := i_2 * 2
    ||
       i_1 := i_1 / (i_2 + 1)
  END
```

```
eval_14 =
  PRE
      card(s_2) > 0
  THEN
       s_1 := s_1 \/ {"test"}
    ||
       s_2 := s_1 /\ s_2
    ||
       i_2 := i_2 * 2
    ||
       i_1 := i_1 / (i_2 + 1)
  END
```

```
eval_15 =
  BEGIN
       s_1 := s_1 \/ s_2
    ||
       s_2 := s_2 - s_1
    ||
       i_1 := i_1 * max(s_1 \/ s_2)
  END
```

```
eval_16 =
  PRE
      card(s_1) > 3 or i_1 > 4
  THEN
       s_1 := s_1 \/ s_2
    ||
       s_2 := s_2 - s_1
    ||
       i_1 := i_1 * max(s_1 \/ s_2)
  END
```

```
eval_17 =
  BEGIN
       i_3 := i_3 ** i_2
    ||
       i_2 := i_2
    ||
       i_1 := i_1 * (i_3 + i_2)
    ||
       i_4 := i_4 + (i_3 - i_1)
  END
```

```
eval_18 =
  BEGIN
       i_3 := i_3 ** i_2
    ||
       i_2 := i_2
    ||
       i_1 := i_1 * (i_3 + i_2)
    ||
       i_6 := i_6 mod i_3
    ||
       i_5 := i_5 - i_3
    ||
       i_4 := i_4 + (i_3 - i_1)
  END
```

```
eval_19 =
  PRE 4 > card(s_1 \/ s_2) THEN
    [...]
```


```
eval_20 =
  PRE s_1 = {} or 0 : s_1 THEN
    [...]
```

```
INVARIANT
    [...] & (s_1 /= {} or {0,1} <<: s_3)    // eval_21
```

```
eval_22 =
  BEGIN
       s_1 := s_1 \/ {i_1}
    ||
       s_2 := s_1 \/ s_2
    ||
       s_3 := s_1 * s_2
    ||
       i_1 := i_1
  END
```

```
eval_23 =
  PRE
      card(s_1) > 0
  THEN
       s_1 := s_1 \/ {i_1}
    ||
       seq_1 := 0 .. i_1 <| seq_1
    ||
       i_1 := i_1 + max(s_1)
  END
```

```
eval_24 =
  PRE (seq_1 = {} => seq_2 \= {}) & i_1 > 0
  [...]
```


```
eval_25 =
  BEGIN
       seq_1 := seq_1 <- int_1
    ||
       seq_2 := dom(seq_1) <| seq_2
    ||
       int_1 := int_1
  END
```

```
INVARIANT
    [...] & (ready /\ waiting) = {}     // eval_26
```

```
INVARIANT
    [...] & active /\ (ready \/ waiting) = {}   // eval_27
```

```
INVARIANT
    [...] & card(active) <= 1   // eval_28
```

```
INVARIANT
    [...] & (active = {}  => ready = {})    // eval_29
```

```
eval_30 = 
  PRE active /= {} & ready = {} THEN
       waiting := waiting \/ active || active := {}
  END
```

```
eval_31(p_PID) = 
  PRE p_PID : PID & p_PID : ready & active /= {} THEN
       waiting := waiting \/ active ||
       active := {p_PID} ||
       ready := ready - {p_PID}
  END
```

```
eval_32(p_PID) = PRE p_PID : PID & p_PID : waiting THEN
        waiting := waiting - { p_PID }
  END
```

```
eval_33(p_PID) = PRE p_PID : PID & p_PID /: active THEN 
        waiting := waiting \/ {p_PID}
  END
```

```
eval_34(p_PID) =
  PRE p_PID : PID & p_PID : waiting & active = {} THEN
       waiting := waiting - {p_PID} ||
       active := {p_PID}
  END
```

```
eval_35(p_PID) =   
  PRE p_PID : PID & p_PID : waiting & active /= {} THEN
       waiting := waiting - {p_PID} ||
       ready := ready \/ {p_PID}
  END
```
