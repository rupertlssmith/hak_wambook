/* Builts-ins for prolog. */
true.

\+(X) :- call(X),!,fail.
\+(X).

=(X, X).

\=(X, Y) :- \+(X = Y).

not(X) :- call(X),!,fail.
not(X).

member(X, [X|_]).
member(X, [_|Tail]) :- member(X, Tail).

append([], List, List).
append([H|Tail1], List2, [H|Tail3]) :- append(Tail1, List2, Tail3).
