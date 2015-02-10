WAM Book Prolog
===============

This is an implementation of the family of logic compilers described in "Warren's Abstract Machine: A Tutorial Reconstruction" by Hassan Aït-Kaci.

A family of logic compilers is described in this book, starting from L0 which introduces unification, through L1, L2 and L3, which build up more of a Prolog implementation, leading the WAM, which is a more complete Prolog implementation.

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

This work was originally produced in order for me to learn how such a compiler works. The code is well documented, and tries to faithfully implement the design described in the book.

This work may be of interest to students of compiler design. In addition to Prolog, you can find examples of parsers, and elements of a modular compiler design that are fairly "text book" in their nature.

Other Sources
-------------

Some parts of the source are outside of this git repository, the Prolog parser in particular, as these are also used in other projects. Take a look at this repository if you want a copy of all of the sources:

    https://github.com/rupertlssmith/lojix

This is current built against version 0.8.11 of lojix, to get the sources for that version checkout its tag:

    > git clone https://github.com/rupertlssmith/lojix.git
    > cd lojix
    > git checkout v0.8.11
