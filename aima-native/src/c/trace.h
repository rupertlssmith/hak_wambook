/* Defines some methods for pretty printing byte code instruction traces. */
#ifndef _TRACE_H
#define _TRACE_H

#ifdef __cplusplus
extern "C" {
#endif

/* A printf function that outputs to stderr instead of stdout. */
void stderrPrintf(__const char *__restrict __format, ...);

/* Callback onto to 'trace' method of obj, for debugging purposes only. */
void traceIt(char* buffer);

/* Callback onto to 'trace' method of obj, for debugging purposes only. */
void trace0(char* mnemonic, int ip);

/* Callback onto to 'trace' method of obj, for debugging purposes only. */
void trace1(char* mnemonic, int ip, int reg1);

/* Callback onto to 'trace' method of obj, for debugging purposes only. */
void trace2(char* mnemonic, int ip, int reg1, signed char mode, int reg2, int ep);

/* Callback onto to 'trace' method of obj, for debugging purposes only. */
void traceFn0(char* mnemonic, int ip, int fn);

/* Callback onto to 'trace' method of obj, for debugging purposes only. */
void traceFn1(char* mnemonic, int ip, int reg1, int fn);

/* Callback onto to 'trace' method of obj, for debugging purposes only. */
void traceConst(char* mnemonic, int ip, int val);

#ifdef __cplusplus
}
#endif

#endif /* _TRACE_H */
