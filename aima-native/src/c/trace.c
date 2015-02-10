#include <stdio.h>
#include "trace.h"

/* Defines the addressing modes. */
#define REG_ADDR 0x01
#define STACK_ADDR 0x02

/* A buffer to hold formatted trace statements in. */
char traceBuffer[250];

/* A printf function that outputs to stderr instead of stdout. */
void stderrPrintf(__const char *__restrict __format, ...)
{
     fprintf(stderr, __format);
}

/* Callback onto to 'trace' method of obj, for debugging purposes only. */
void traceIt(char* buffer)
{
     fprintf(stderr, "%s\n", buffer);
}

/* Callback onto to 'trace' method of obj, for debugging purposes only. */
void trace0(char* mnemonic, int ip)
{
     sprintf(traceBuffer, "%i: %s", ip, mnemonic);
     traceIt(traceBuffer);
}

/* Callback onto to 'trace' method of obj, for debugging purposes only. */
void trace1(char* mnemonic, int ip, int reg1)
{
     sprintf(traceBuffer, "%i: %s X%i", ip, mnemonic, reg1);
     traceIt(traceBuffer);
}

/* Callback onto to 'trace' method of obj, for debugging purposes only. */
void trace2(char* mnemonic, int ip, int reg1, signed char mode, int reg2, int ep)
{
     int reg;

     if (mode == REG_ADDR)
     {
          sprintf(traceBuffer, "%i: %s X%i, A%i", ip, mnemonic, reg1, reg2);
     }
     else
     {
          reg = (reg1 - ep - 3);
          sprintf(traceBuffer, "%i: %s Y%i, A%i", ip, mnemonic, reg, reg2);
     }

     traceIt(traceBuffer);
}

/* Callback onto to 'trace' method of obj, for debugging purposes only. */
void traceFn0(char* mnemonic, int ip, int fn)
{
     sprintf(traceBuffer, "%i: %s %i", ip, mnemonic, fn);
     traceIt(traceBuffer);
}

/* Callback onto to 'trace' method of obj, for debugging purposes only. */
void traceFn1(char* mnemonic, int ip, int reg1, int fn)
{
     sprintf(traceBuffer, "%i: %s X%i,%i", ip, mnemonic, reg1, fn);
     traceIt(traceBuffer);
}

/* Callback onto to 'trace' method of obj, for debugging purposes only. */
void traceConst(char* mnemonic, int ip, int val)
{
     sprintf(traceBuffer, "%i: %s %i", ip, mnemonic, val);
     traceIt(traceBuffer);
}
