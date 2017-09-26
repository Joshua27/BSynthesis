# Repair and Generation of Formal Models Using Synthesis

[![Build Status](https://travis-ci.org/Joshua27/BSynthesis.svg?branch=master)](https://travis-ci.org/Joshua27/BSynthesis)
[![Dependency Status](https://www.versioneye.com/user/projects/592ac926a8a056006137f3fb/badge.svg?style=flat-square)](https://www.versioneye.com/user/projects/592ac926a8a056006137f3fb)
[![Sonarqube Quality Gate](https://sonarqube.com/api/badges/gate?key=de.hhu.stups.bsynthesis)](https://sonarqube.com/dashboard?id=de.hhu.stups.bsynthesis)
[![Sonarqube Coverage](https://sonarqube.com/api/badges/measure?key=de.hhu.stups.bsynthesis&metric=coverage)](https://sonarqube.com/component_measures/domain/Coverage?id=de.hhu.stups.bsynthesis)
[![Sonarqube Tech Debt](https://sonarqube.com/api/badges/measure?key=de.hhu.stups.bsynthesis&metric=sqale_debt_ratio)](https://sonarqube.com/component_measures/domain/Maintainability?id=de.hhu.stups.bsynthesis)

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

Hence, we propose partially automating the development process of formal models using software synthesis:
Guided by examples of positive and negative behavior we strengthen preconditions or relax invariants appropriately.
Moreover, by collecting initial examples from the user we generate operations from scratch or adapt existing ones.
All this is done using constant user feedback, yielding an interactive assistant.

# Requirements

- Java 8 Oracle JDK
- Gradle 3.2.1 (or newer)

# Start the Application

From the root folder: `gradle run`

Or download the latest release.

# References

The synthesis technique in use is based on the one by Jha et.al. introduced in ["Oracle-guided component-based program synthesis"](https://people.eecs.berkeley.edu/~sseshia/pubdir/icse10-TR.pdf), Proceedings ICSE, 2010.
