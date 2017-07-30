# BSynthesis

[![Build Status](https://travis-ci.org/Joshua27/BSynthesis.svg?branch=master)](https://travis-ci.org/Joshua27/BSynthesis)
[![Dependency Status](https://www.versioneye.com/user/projects/592ac926a8a056006137f3fb/badge.svg?style=flat-square)](https://www.versioneye.com/user/projects/592ac926a8a056006137f3fb)
[![Sonarqube Quality Gate](https://sonarqube.com/api/badges/gate?key=de.hhu.stups.bsynthesis)](https://sonarqube.com/dashboard?id=de.hhu.stups.bsynthesis)
[![Sonarqube Coverage](https://sonarqube.com/api/badges/measure?key=de.hhu.stups.bsynthesis&metric=coverage)](https://sonarqube.com/component_measures/domain/Coverage?id=de.hhu.stups.bsynthesis)
[![Sonarqube Tech Debt](https://sonarqube.com/api/badges/measure?key=de.hhu.stups.bsynthesis&metric=sqale_debt_ratio)](https://sonarqube.com/component_measures/domain/Maintainability?id=de.hhu.stups.bsynthesis)

Writing a formal model is a complicated and time-consuming task. Usually, one succes-
sively refines a model with the help of provers or model checkers. In case an invariant
violation is found or if a proof fails, the model has to be adapted. However, in general,
finding the appropriate set of changes is non-trivial.
We propose partially automating the process using software synthesis: Guided by exam-
ples of positive and negative behavior we strengthen preconditions or relax invariants
appropriately. Moreover, by collecting initial examples from the user we generate opera-
tions from scratch or adapt existing ones.
All this is done using constant user feedback, yielding an interactive assistant.

The synthesis technique in use is based on the one by Jha et.al. introduced in ["Oracle-guided component-based program synthesis"](https://people.eecs.berkeley.edu/~sseshia/pubdir/icse10-TR.pdf), Proceedings ICSE, 2010. 
