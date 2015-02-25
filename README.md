WAM Book Prolog
===============

This is an implementation of the family of logic compilers described in "Warren's Abstract Machine: A Tutorial Reconstruction" by Hassan Aït-Kaci.

A family of logic compilers is described in this book, starting from L0 which introduces unification, through L1, L2 and L3, which build up more of a Prolog implementation, leading the WAM, which is a more complete Prolog implementation and optimizes the simpler designs of the earlier languages.

A copy of the WAM book, with errata, can be found here:
    
    https://github.com/a-yiorgos/wambook    

Getting Started
---------------

Ensure you have Apache Maven 3.2.2 or later installed.

Build with:

    > mvn clean install

Run the WAM implementation with:

    > cd target
    > chmod +x wam
    > ./wam
    
Audience
--------

This work may be of interest to students of compiler design. The code is well documented, and tries to faithfully implement the design described in the book.
    
This work was originally produced in order for me to learn how such a compiler works, as well as to provide a basis for experimenting with variations on logic programming languages.

In addition to Prolog, you can find examples of parsers, and elements of a modular compiler design that are fairly "text book" in their nature. The symbol table design was heavily influenced by one described in "Engineering A Compiler", by Keith Cooper and Linda Torczon, and could be re-used in other compiler projects.

State of the work
-----------------

The languages L0 through L3 are complete as per the book.

The WAM machine is missing the indexing scheme. This may get completed at a later date, especially if someone were to contribute it.

The languages L0 through L2 also have native byte code machines implemented in C. Given the state-of-the-art in Java compilations and virtual machines, these are unlikely to offer a performance advantage over the machines implemented in Java.

The language L2 has a JIT compilation machine implemented on top of LLVM. This is not 100% complete, but provides a decent introduction to compiling down to native code.
    
Other Sources
-------------

Some parts of the source are outside of this git repository, the Prolog parser in particular, as these are also used in other projects. Take a look at this repository if you want a copy of all of the sources:

    https://github.com/rupertlssmith/lojix

This is currently built against version 0.8.11 of lojix, to get the sources for that version checkout its tag:

    > git clone https://github.com/rupertlssmith/lojix.git
    > cd lojix
    > git checkout v0.8.11
