#include <stdlib.h>
#include <stdio.h>
#include "com_thesett_aima_logic_fol_l1_L1UnifyingNativeMachine.h"

/** Defines the machine instruction types. */
#define PUT_STRUC 0x01
#define SET_VAR 0x02
#define SET_VAL 0x03
#define GET_STRUC 0x04
#define UNIFY_VAR 0x05
#define UNIFY_VAL 0x06
#define PUT_VAR 0x07
#define PUT_VAL 0x08
#define GET_VAR 0x09
#define GET_VAL 0x0a
#define CALL 0x0b
#define PROCEED 0x0c

/** Defines the heap cell marker types. */
#define REF 0x01
#define STR 0x02

/* Defines the heap size to use for the virtual machine. */
#define HEAP_SIZE 10000

/* Defines the register capacity for the virtual machine. */
#define REG_SIZE 10

/* Defines the max unification stack depth for the virtual machine. */
#define USTACK_SIZE 1000

typedef struct
{
     /* Holds the current instruction pointer into the code. */
     jint ip;

     /* Holds the working heap. */
     jint *heap;

     /* Holds the heap pointer. */
     jint hp;

     /* Holds the secondary heap pointer, used for the heap address of the next term to match. */
     jint sp;

     /* Holds the unification stack. */
     jint *ustack;

     /* Holds the unification stack pointer. */
     jint up;

     /* Used to record whether the machine is in structure read or write mode. */
     jboolean writeMode;

     /* Holds the heap cell tag from the most recent dereference. */
     jbyte derefTag;

     /* Holds the heap call value from the most recent dereference. */
     jint derefVal;
} l1MachineState;

l1MachineState *l1state;

/*
 * Pushes a value onto the unification stack.
 *
 * @param val The value to push onto the stack.
 */
void l1uPush(jint val)
{
     l1state->ustack[++(l1state->up)] = val;
}

/*
 * Pops a value from the unification stack.
 *
 * @return The top value from the unification stack.
 */
jint l1uPop()
{
     return l1state->ustack[(l1state->up)--];
}

/*
 * Clears the unification stack.
 */
void l1uClear()
{
     l1state->up = -1;
}

/*
 * Checks if the unification stack is empty.
 *
 * @return <tt>true</tt> if the unification stack is empty, <tt>false</tt> otherwise.
 */
jboolean l1uEmpty()
{
     return l1state->up == -1 ? JNI_TRUE : JNI_FALSE;
}

/*
 * Dereferences a heap pointer (or register), returning the address that it refers to after following all
 * reference chains to their conclusion. This method is also side effecting, in that the contents of the
 * refered to heap cell are also loaded into the fields {@link #derefTag} and {@link #derefVal}.
 *
 * @param a The address to dereference.
 *
 * @return The address that the reference refers to.
 */
jint l1deref(jint a)
{
     jint addr;
     jint tmp;
     jbyte derefTag;
     jint derefVal;

     //printf("jint deref(jint a = 0x%02x): called\n", a);

     // tag, value <- STORE[a]
     addr = a;
     tmp = l1state->heap[a];
     derefTag = (jbyte)((tmp & 0xFF000000) >> 24);
     derefVal = tmp & 0x00FFFFFF;

     // while tag = REF and value != a
     while ((derefTag == REF))
     {
          // tag, value <- STORE[a]
          addr = derefVal;
          tmp = l1state->heap[derefVal];
          derefTag = (jbyte)((tmp & 0xFF000000) >> 24);
          tmp = tmp & 0x00FFFFFF;

          // Break on free var.
          if (derefVal == tmp)
          {
               break;
          }

          derefVal = tmp;
     }

     //printf("derefTag = 0x%02x\n", derefTag);
     //printf("derefVal = 0x%02x\n", derefVal);
     //printf("addr = 0x%02x\n", addr);

     l1state->derefTag = derefTag;
     l1state->derefVal = derefVal;

     return addr;
}

/*
 * Attempts to unify structures or references on the heap, given two references to them. Structures are matched
 * element by element, free references become bound.
 *
 * @param a1 The address of the first structure or reference.
 * @param a2 The address of the second structure or reference.
 *
 * @return <tt>true</tt> if the two structures unify, <tt>false</tt> otherwise.
 */
jboolean l1unify(jint a1, jint a2)
{
     jboolean fail;

     //printf("jboolean unify(jint a1 = 0x%02x, jint a2 = 0x%02x): called\n", a1, a2);

     // pdl.push(a1)
     // pdl.push(a2)
     l1uPush(a1);
     l1uPush(a2);

     // fail <- false
     fail = JNI_FALSE;

     // while !empty(PDL) and not failed
     while (l1uEmpty() == JNI_FALSE && !fail)
     {
          // d1 <- deref(pdl.pop())
          // d2 <- deref(pdl.pop())
          // t1, v1 <- STORE[d1]
          // t2, v2 <- STORE[d2]
          jint d1 = l1deref(l1uPop());
          jint t1 = l1state->derefTag;
          jint v1 = l1state->derefVal;

          int d2 = l1deref(l1uPop());
          int t2 = l1state->derefTag;
          int v2 = l1state->derefVal;

          // if (d1 != d2)
          if (d1 != d2)
          {
               // if (t1 = REF or t2 = REF)
               // bind(d1, d2)
               if ((t1 == REF))
               {
                    l1state->heap[d1] = (REF << 24) | (d2 & 0xFFFFFF);
               }
               else if (t2 == REF)
               {
                    l1state->heap[d2] = (REF << 24) | (d1 & 0xFFFFFF);
               }
               else
               {
                    // f1/n1 <- STORE[v1]
                    // f2/n2 <- STORE[v2]
                    jint f_n1 = l1state->heap[v1];
                    jint f_n2 = l1state->heap[v2];
                    jbyte n1 = (jbyte)(f_n1 & 0xFF);

                    // if f1 = f2 and n1 = n2
                    if (f_n1 == f_n2)
                    {
                         // for i <- 1 to n1
                         jint i;
                         for (i = 1; i <= n1; i++)
                         {
                              // pdl.push(v1 + i)
                              // pdl.push(v2 + i)
                              l1uPush(v1 + i);
                              l1uPush(v2 + i);
                         }
                    }
                    else
                    {
                         // fail <- true
                         fail = JNI_TRUE;
                    }
               }
          }
     }

     return fail == JNI_TRUE ? JNI_FALSE : JNI_TRUE;
}

/*
 * Resets the machine to its initial state. This clears any programs from the machine, and clears all of its stacks
 * and heaps.
 *
 * Class:     com_thesett_aima_logic_fol_l1_L1UnifyingNativeMachine
 * Method:    reset
 * Signature: ()V
 *
 * @param env     The native code execution environment.
 * @param obj     The object that is the context to this native method.
 * @param offset  The offset within the code buffer of the byte code to execute.
 */
JNIEXPORT void JNICALL Java_com_thesett_aima_logic_fol_l1_L1UnifyingNativeMachine_nativeReset
(JNIEnv * env, jobject obj)
{
     // Allocate space for the machines state.
     l1state = malloc(sizeof(l1MachineState));

     // Create fresh heaps, code areas and stacks.
     l1state->heap = malloc((REG_SIZE + HEAP_SIZE) * sizeof(jint));
     l1state->ustack = malloc(USTACK_SIZE * sizeof(jint));

     // Registers are on the top of the heap, so initialize the heap pointers to the heap area.
     l1state->hp = REG_SIZE;
     l1state->sp = REG_SIZE;

     // Clear the unification stack.
     l1state->up = -1;

     // Turn off write mode.
     l1state->writeMode = JNI_FALSE;

     // Reset the instruction pointer to that start of the code area.
     l1state->ip = 0;

     // Could probably not bother resetting these, but will do it anyway just to be sure.
     l1state->derefTag = 0;
     l1state->derefVal = 0;
}

/*
 * Executes a compiled functor returning an indication of whether or not a unification was found.
 *
 * @param env     The native code execution environment.
 * @param obj     The object that is the context to this native method.
 * @param offset  The offset within the code buffer of the byte code to execute.
 *
 * @return <tt>true</tt> if a unification was found, <tt>false</tt> if the search for one failed.
 */
JNIEXPORT jboolean JNICALL Java_com_thesett_aima_logic_fol_l1_L1UnifyingNativeMachine_execute
(JNIEnv * env, jobject object, jobject codeBuf, jint offset)
{
     jint addr;
     jbyte tag;
     jint a;

     jint hp = l1state->hp;
     jint sp = l1state->sp;
     jboolean writeMode = l1state->writeMode;
     jint ip;

     jboolean failed = JNI_FALSE;

     jboolean complete = JNI_FALSE;

     jsize length = (*env)->GetDirectBufferCapacity(env, codeBuf);
     jbyte * code = (*env)->GetDirectBufferAddress(env, codeBuf);

     ip = offset;
     l1uClear();

     //printf("\nJNIEXPORT jboolean JNICALL Java_com_thesett_aima_logic_fol_l1_L1UnifyingNativeMachine_execute: called\n");

     while (failed == JNI_FALSE && complete == JNI_FALSE && (ip < length))
     {
          // Grab next instruction and switch on it.
          jbyte instruction = code[ip++];
          jint xi = (jint)code[ip++];

          switch (instruction)
          {
               // put_struc xi:
          case PUT_STRUC:
          {
               // grab f/n
               jint f_n = *(jint*)(code + ip);
               ip += 4;

               //printf("0x%02x: PUT_STRUC X%i,%i (0x%02x, 0x%02x)\n", (ip - 6), xi, f_n, xi, f_n);

               // heap[h] <- STR, h + 1
               l1state->heap[hp] = (STR << 24) | ((hp + 1) & 0xFFFFFF);

               // heap[h+1] <- f/n
               l1state->heap[hp + 1] = f_n;

               // xi <- heap[h]
               l1state->heap[xi] = l1state->heap[hp];

               // h <- h + 2
               hp += 2;

               break;
          }

          // set_var xi:
          case SET_VAR:
          {
               //printf("0x%02x: SET_VAR X%i (0x%02x)\n", (ip - 2), xi, xi);

               // heap[h] <- REF, h
               l1state->heap[hp] = (REF << 24) | (hp & 0xFFFFFF);

               // xi <- heap[h]
               l1state->heap[xi] = l1state->heap[hp];

               // h <- h + 1
               hp++;

               break;
          }

          // set_val xi:
          case SET_VAL:
          {
               //printf("0x%02x: SET_VAL %i  (0x%02x)\n", (ip - 2), xi, xi);

               // heap[h] <- xi
               l1state->heap[hp] = l1state->heap[xi];

               // h <- h + 1
               hp++;

               break;
          }

          // get_struc xi,
          case GET_STRUC:
          {
               // grab f/n
               jint f_n = *(jint*)(code + ip);
               ip += 4;

               //printf("0x%02x: GET_STRUC X%i,%i (0x%02x, 0x%02x)\n", (ip - 6), xi, f_n, xi, f_n);

               // addr <- deref(xi);
               addr = l1deref(xi);

               // switch STORE[addr]
               //int tmp = heap[addr];
               //byte tag = (byte)((tmp & 0xFF000000) >> 24);
               //int a = tmp & 0x00FFFFFF;
               tag = l1state->derefTag;
               a = l1state->derefVal;

               //printf("a = 0x%02x\n", a);

               switch (tag)
               {
                    // case REF:
               case REF:
               {
                    //printf("tag = REF\n");

                    // heap[h] <- STR, h + 1
                    l1state->heap[hp] = (STR << 24) | ((hp + 1) & 0xFFFFFF);

                    // heap[h+1] <- f/n
                    l1state->heap[hp + 1] = f_n;

                    // bind(addr, h)
                    l1state->heap[addr] = (REF << 24) | (hp & 0xFFFFFF);

                    // h <- h + 2
                    hp += 2;

                    // mode <- write
                    writeMode = JNI_TRUE;

                    break;
               }

               // case STR, a:
               case STR:
               {
                    //printf("tag = STR\n");

                    // if heap[a] = f/n
                    //printf("heap[a] = %i (0x%02x)\n", heap[a], heap[a]);
                    //printf("f_n = %i (0x%02x)\n", f_n, f_n);

                    if (l1state->heap[a] == f_n)
                    {
                         // s <- a + 1
                         sp = a + 1;

                         // mode <- read
                         writeMode = JNI_FALSE;
                    }
                    else
                    {
                         // fail
                         //printf("failed\n");
                         failed = JNI_TRUE;
                    }

                    break;
               }
               }

               break;
          }

          // unify_var xi:
          case UNIFY_VAR:
          {
               //printf("0x%02x: UNIFY_VAR X%i (0x%02x)\n", (ip - 2), xi, xi);

               // switch mode
               if (writeMode == JNI_FALSE)
               {
                    // case read:
                    // xi <- heap[s]
                    l1state->heap[xi] = l1state->heap[sp];
               }
               else
               {
                    // case write:
                    // heap[h] <- REF, h
                    l1state->heap[hp] = (REF << 24) | (hp & 0xFFFFFF);

                    // xi <- heap[h]
                    l1state->heap[xi] = l1state->heap[hp];

                    // h <- h + 1
                    hp++;
               }

               // s <- s + 1
               sp++;

               break;
          }

          // unify_val xi:
          case UNIFY_VAL:
          {
               //printf("0x%02x: UNIFY_VAL X%i (0x%02x)\n", (ip - 2), xi, xi);

               // switch mode
               if (writeMode == JNI_FALSE)
               {
                    // case read:
                    // unify (xi, s)
                    failed = l1unify(xi, sp) == JNI_TRUE ? JNI_FALSE : JNI_TRUE;
               }
               else
               {
                    // case write:
                    // heap[h] <- xi
                    l1state->heap[hp] = l1state->heap[xi];

                    // h <- h + 1
                    hp++;
               }

               // s <- s + 1
               sp++;

               break;
          }


          // put_var Xn, Ai:
          case PUT_VAR:
          {
               // grab Ai
               jbyte ai = (jint)code[ip++];

               //printf("0x%02x: PUT_VAR X%i, A%i (0x%02x, 0x%02x)\n", (ip - 3), xi, ai, xi, ai);

               // heap[h] <- REF, H
               l1state->heap[hp] = (REF << 24) | (hp & 0xFFFFFF);

               // Xn <- heap[h]
               l1state->heap[xi] = l1state->heap[hp];

               // Ai <- heap[h]
               l1state->heap[ai] = l1state->heap[hp];

               // h <- h + 1
               hp++;

               break;
          }

          // put_val Xn, Ai:
          case PUT_VAL:
          {
               // grab Ai
               jbyte ai = (jint)code[ip++];

               //printf("0x%02x: PUT_VAL X%i, A%i (0x%02x, 0x%02x)\n", (ip - 3), xi, ai, xi, ai);

               // Ai <- Xn
               l1state->heap[ai] = l1state->heap[xi];

               break;
          }

          // get var Xn, Ai:
          case GET_VAR:
          {
               // grab Ai
               jbyte ai = (jint)code[ip++];

               //printf("0x%02x: GET_VAR X%i, A%i (0x%02x, 0x%02x)\n", (ip - 3), xi, ai, xi, ai);

               // Xn <- Ai
               l1state->heap[xi] = l1state->heap[ai];

               break;
          }

          // get_val Xn, Ai:
          case GET_VAL:
          {
               // grab Ai
               jbyte ai = (jint)code[ip++];

               //printf("0x%02x: GET_VAL X%i, A%i (0x%02x, 0x%02x)\n", (ip - 3), xi, ai, xi, ai);

               // unify (Xn, Ai)
               failed = l1unify(xi, ai) == JNI_TRUE ? JNI_FALSE : JNI_TRUE;

               break;
          }

          // call @(p/n)
          case CALL:
          {
               // grab @(p/n) (ip is decremented here, because already took first byte of the address as xi).
               int p_n = *(jint*)(code + ip - 1);

               //printf("0x%02x: CALL %i, (0x%02x)\n", (ip - 2), p_n, p_n);

               // Ensure that the predicate to call is known and linked int, otherwise fail.
               if (p_n == -1)
               {
                    failed = JNI_TRUE;

                    break;
               }

               // ip <- @(p/n)
               ip = p_n;

               break;
          }

          case PROCEED:
          {
               //printf("0x%02x: PROCEED\n", (ip - 2));
               // noop.

               complete = JNI_TRUE;
               break;
          }
          }
     }

     // Preserve the current state of the machine.
     l1state->hp = hp;
     l1state->sp = sp;
     l1state->writeMode = writeMode;

     return failed == JNI_TRUE ? JNI_FALSE : JNI_TRUE;
}

/*
 * Dereferences a heap pointer (or register), returning the address that it refers to after following all
 * reference chains to their conclusion. This method is also side effecting, in that the contents of the
 * refered to heap cell are also loaded into the fields {@link #derefTag} and {@link #derefVal}.
 *
 * @param a The address to dereference.
 *
 * @return The address that the reference refers to.
 */
JNIEXPORT jint JNICALL Java_com_thesett_aima_logic_fol_l1_L1UnifyingNativeMachine_deref
(JNIEnv * env, jobject obj, jint a)
{
     return l1deref(a);
}

/*
 * Gets the heap cell tag for the most recent dereference operation.
 *
 * @return The heap cell tag for the most recent dereference operation.
 */
JNIEXPORT jbyte JNICALL Java_com_thesett_aima_logic_fol_l1_L1UnifyingNativeMachine_getDerefTag
(JNIEnv * env, jobject obj)
{
     return l1state->derefTag;
}

/*
 * Gets the heap cell value for the most recent dereference operation.
 *
 * @return The heap cell value for the most recent dereference operation.
 */
JNIEXPORT jint JNICALL Java_com_thesett_aima_logic_fol_l1_L1UnifyingNativeMachine_getDerefVal
(JNIEnv * env, jobject obj)
{
     return l1state->derefVal;
}

/*
 * Gets the value of the heap cell at the specified location.
 *
 * @param addr The address to fetch from the heap.
 * @return The heap cell at the specified location.
 */
JNIEXPORT jint JNICALL Java_com_thesett_aima_logic_fol_l1_L1UnifyingNativeMachine_getHeap
(JNIEnv * env, jobject obj, jint addr)
{
     return l1state->heap[addr];
}
