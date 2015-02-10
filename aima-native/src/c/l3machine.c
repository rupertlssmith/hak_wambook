#include <stdlib.h>
#include <stdio.h>
#include "com_thesett_aima_logic_fol_l3_L3ResolvingNativeMachine.h"
#include "trace.h"

/* Defines the machine instruction types. */
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
#define ALLOCATE 0x0d
#define DEALLOCATE 0x0e

/* Defines the addressing modes. */
#define REG_ADDR 0x01
#define STACK_ADDR 0x02

/* Defines the heap cell marker types. */
#define REF 0x01
#define STR 0x02

/* Defines the register capacity for the virtual machine. */
#define REG_SIZE 10

/* Defines the offset of the first register in the data area. */
#define REG_BASE 0

/* Defines the heap size to use for the virtual machine. */
#define HEAP_SIZE 10000

/* Defines the offset of the base of the heap in the data area. */
#define HEAP_BASE REG_SIZE

/* Defines the stack size to use for the virtual machine. */
#define STACK_SIZE 10000

/* Defines the offset of the base of the stack in the data area. */
#define STACK_BASE (REG_SIZE + HEAP_SIZE)

/* Defines the max unification stack depth for the virtual machine. */
#define PDL_SIZE 1000

/* Defines the offset of the base of the PDL in the data area. */
#define PDL_BASE (REG_SIZE + HEAP_SIZE + STACK_SIZE)

/* Defines the highest address in the data area of the virtual machine. */
#define TOP (REG_SIZE + HEAP_SIZE + STACK_SIZE + PDL_SIZE)

typedef struct
{
     /* Holds the current instruction pointer into the code. */
     jint ip;

     /* Holds the current continuation point. */
     jint cp;

     /* Holds the enire data segment of the machine. All registers, heaps and stacks are held in here. */
     jint *data;

     /* Holds the heap pointer. */
     jint hp;

     /* Holds the secondary heap pointer, used for the heap address of the next term to match. */
     jint sp;

     /* Holds the unification stack pointer. */
     jint up;

     /** Holds the environment base pointer. */
     jint ep;

     /** Holds the environment, top-of-stack pointer. */
     jint esp;

     /* Used to record whether the machine is in structure read or write mode. */
     jboolean writeMode;

     /* Holds the heap cell tag from the most recent dereference. */
     jbyte derefTag;

     /* Holds the heap call value from the most recent dereference. */
     jint derefVal;
} l3MachineState;

/* Holds the machine state. */
l3MachineState *l3state;

/*
 * Pushes a value onto the unification stack.
 *
 * @param val The value to push onto the stack.
 */
void l3uPush(jint val)
{
     l3state->data[--(l3state->up)] = val;
}

/*
 * Pops a value from the unification stack.
 *
 * @return The top value from the unification stack.
 */
jint l3uPop()
{
     return l3state->data[(l3state->up)++];
}

/*
 * Clears the unification stack.
 */
void l3uClear()
{
     l3state->up = TOP;
}

/*
 * Checks if the unification stack is empty.
 *
 * @return <tt>true</tt> if the unification stack is empty, <tt>false</tt> otherwise.
 */
jboolean l3uEmpty()
{
     return l3state->up >= TOP ? JNI_TRUE : JNI_FALSE;
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
jint l3deref(jint a)
{
     jint addr;
     jint tmp;
     jbyte derefTag;
     jint derefVal;

     // printf("jint l3deref(jint a = 0x%02x): called\n", a);

     // tag, value <- STORE[a]
     addr = a;
     tmp = l3state->data[a];
     derefTag = (jbyte)((tmp & 0xFF000000) >> 24);
     derefVal = tmp & 0x00FFFFFF;

     // printf("derefTag = 0x%02x\n", derefTag);
     // printf("derefVal = 0x%02x\n", derefVal);
     // printf("addr = 0x%02x\n", addr);

     // while tag = REF and value != a
     while ((derefTag == REF))
     {
          // printf("while ((derefTag == REF))\n");

          // tag, value <- STORE[a]
          addr = derefVal;
          tmp = l3state->data[derefVal];
          derefTag = (jbyte)((tmp & 0xFF000000) >> 24);
          tmp = tmp & 0x00FFFFFF;

          // Break on free var.
          if (derefVal == tmp)
          {
               break;
          }

          derefVal = tmp;

          // printf("derefTag = 0x%02x\n", derefTag);
          // printf("derefVal = 0x%02x\n", derefVal);
          // printf("addr = 0x%02x\n", addr);
     }

     l3state->derefTag = derefTag;
     l3state->derefVal = derefVal;

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
jboolean l3unify(jint a1, jint a2)
{
     jboolean fail;

     // printf("jboolean unify(jint a1 = 0x%02x, jint a2 = 0x%02x): called\n", a1, a2);

     // pdl.push(a1)
     // pdl.push(a2)
     l3uPush(a1);
     l3uPush(a2);

     // fail <- false
     fail = JNI_FALSE;

     // while !empty(PDL) and not failed
     while (l3uEmpty() == JNI_FALSE && !fail)
     {
          // printf("while (l3uEmpty() == JNI_FALSE && !fail)\n");

          // d1 <- deref(pdl.pop())
          // d2 <- deref(pdl.pop())
          // t1, v1 <- STORE[d1]
          // t2, v2 <- STORE[d2]
          jint d1 = l3deref(l3uPop());
          jint t1 = l3state->derefTag;
          jint v1 = l3state->derefVal;

          jint d2 = l3deref(l3uPop());
          jint t2 = l3state->derefTag;
          jint v2 = l3state->derefVal;

          // printf("d1 = %i\n", d1);
          // printf("t1 = %i\n", t1);
          // printf("v1 = %i\n", v1);

          // printf("d2 = %i\n", d2);
          // printf("t2 = %i\n", t2);
          // printf("v2 = %i\n", v2);

          // if (d1 != d2)
          if (d1 != d2)
          {
               // printf("if (d1 != d2)\n");

               // if (t1 = REF or t2 = REF)
               // bind(d1, d2)
               if ((t1 == REF))
               {
                    // printf("if ((t1 == REF))\n");

                    l3state->data[d1] = (REF << 24) | (d2 & 0xFFFFFF);
               }
               else if (t2 == REF)
               {
                    // printf("else if (t2 == REF)\n");

                    l3state->data[d2] = (REF << 24) | (d1 & 0xFFFFFF);
               }
               else
               {
                    // printf("else\n");

                    // printf("v1 = %i\n", v1);
                    // printf("v2 = %i\n", v2);

                    // f1/n1 <- STORE[v1]
                    // f2/n2 <- STORE[v2]
                    jint f_n1 = l3state->data[v1];
                    // printf("f_n1 = %i\n", f_n1);
                    jint f_n2 = l3state->data[v2];
                    // printf("f_n2 = %i\n", f_n2);
                    jbyte n1 = (jbyte)(f_n1 & 0xFF);
                    // printf("n1 = %i\n", n1);

                    // if f1 = f2 and n1 = n2
                    if (f_n1 == f_n2)
                    {
                         // printf("if (f_n1 == f_n2)\n");

                         // for i <- 1 to n1
                         jint i;
                         for (i = 1; i <= n1; i++)
                         {
                              // printf("for (i = 1; i <= n1; i++)\n");

                              // pdl.push(v1 + i)
                              // pdl.push(v2 + i)
                              l3uPush(v1 + i);
                              l3uPush(v2 + i);
                         }
                    }
                    else
                    {
                         // printf("else\n");

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
 * Class:     com_thesett_aima_logic_fol_l3_L3ResolvingNativeMachine
 * Method:    reset
 * Signature: ()V
 *
 * @param env     The native code execution environment.
 * @param obj     The object that is the context to this native method.
 * @param offset  The offset within the code buffer of the byte code to execute.
 */
JNIEXPORT void JNICALL Java_com_thesett_aima_logic_fol_l3_L3ResolvingNativeMachine_nativeReset
(JNIEnv * env, jobject obj)
{
     int i;

     // printf("JNIEXPORT void JNICALL Java_com_thesett_aima_logic_fol_l3_L3ResolvingNativeMachine_nativeReset: called\n");

     // Allocate space for the machines state.
     l3state = malloc(sizeof(l3MachineState));

     // Create fresh heaps and stacks.
     l3state->data = malloc((TOP) * sizeof(jint));

     for (i = 0; i < TOP; i++)
     {
          l3state->data[i] = 0;
     }

     // Registers are on the top of the heap, so initialize the heap pointers to the heap area.
     l3state->hp = REG_SIZE;
     l3state->sp = REG_SIZE;

     // The stack comes after the heap.
     l3state->ep = REG_SIZE + HEAP_SIZE;
     l3state->esp = l3state->ep;

     // The unification stack is a push down stack at the end of the data area.
     l3state->up = TOP;

     // Turn off write mode.
     l3state->writeMode = JNI_FALSE;

     // Reset the instruction pointer to that start of the code area.
     l3state->ip = 0;
     l3state->cp = 0;

     // Could probably not bother resetting these, but will do it anyway just to be sure.
     l3state->derefTag = 0;
     l3state->derefVal = 0;
}

/*
 * Notified whenever code is added to the machine. This provides a hook in point at which the machine may,
 * if required, compile the code down below the byte code level.
 *
 * @param codeBuffer The code buffer.
 * @param codeOffset The start offset of the new code.
 * @param length     The length of the new code.
 */
JNIEXPORT void JNICALL Java_com_thesett_aima_logic_fol_l3_L3ResolvingNativeMachine_codeAdded
(JNIEnv * env, jobject object, jobject codeBuf, jint offset, jint length)
{
}

/*
 * Executes a compiled functor returning an indication of whether or not a unification was found.
 *
 * @param env     The native code execution environment.
 * @param obj     The object that is the context to this native method.
 * @param codeBuf A direct buffer containing the byte code to execute.
 * @param offset  The offset within the code buffer of the byte code to execute.
 *
 * @return <tt>true</tt> if a unification was found, <tt>false</tt> if the search for one failed.
 */
JNIEXPORT jboolean JNICALL Java_com_thesett_aima_logic_fol_l3_L3ResolvingNativeMachine_execute
(JNIEnv * env, jobject object, jobject codeBuf, jint offset)
{
     jint addr;
     jbyte tag;
     jint a;

     jint hp = l3state->hp;
     jint sp = l3state->sp;
     jboolean writeMode = l3state->writeMode;
     jint ip;
     jint cp;
     jint ep = l3state->ep;
     jint esp = l3state->esp;

     jboolean failed = JNI_FALSE;

     jsize length = (*env)->GetDirectBufferCapacity(env, codeBuf);
     jbyte * code = (*env)->GetDirectBufferAddress(env, codeBuf);

     traceIt("\nL3 Execute\n");

     // Start execution at the requested address.
     ip = offset;

     // Set the initial CP to point to the end of the code, used as a termination condition.
     cp = length;
     l3uClear();

     while (failed == JNI_FALSE && (ip < length))
     {
          // Grab next instruction and switch on it.
          jbyte instruction = code[ip];

          switch (instruction)
          {
               // put_struc xi:
          case PUT_STRUC:
          {
               // grab f/n
               jbyte mode = code[ip + 1];
               jint xi = (jint)code[ip + 2] + ((mode == STACK_ADDR) ? (ep + 3) : 0);
               jint f_n = *(jint*)(code + ip + 3);

               traceFn1("PUT_STRUC", ip, xi, f_n);

               // heap[h] <- STR, h + 1
               l3state->data[hp] = (STR << 24) | ((hp + 1) & 0xFFFFFF);

               // heap[h+1] <- f/n
               l3state->data[hp + 1] = f_n;

               // xi <- heap[h]
               l3state->data[xi] = l3state->data[hp];

               // h <- h + 2
               hp += 2;

               // P <- instruction_size(P)
               ip += 7;

               break;
          }

          // set_var xi:
          case SET_VAR:
          {
               jbyte mode = code[ip + 1];
               jint xi = (jint)code[ip + 2] + ((mode == STACK_ADDR) ? (ep + 3) : 0);

               trace1("SET_VAR", ip, xi);

               // heap[h] <- REF, h
               l3state->data[hp] = (REF << 24) | (hp & 0xFFFFFF);

               // xi <- heap[h]
               l3state->data[xi] = l3state->data[hp];

               // h <- h + 1
               hp++;

               // P <- instruction_size(P)
               ip += 3;

               break;
          }

          // set_val xi:
          case SET_VAL:
          {
               jbyte mode = code[ip + 1];
               jint xi = (jint)code[ip + 2] + ((mode == STACK_ADDR) ? (ep + 3) : 0);

               trace1("SET_VAL", ip, xi);

               // heap[h] <- xi
               l3state->data[hp] = l3state->data[xi];

               // h <- h + 1
               hp++;

               // P <- instruction_size(P)
               ip += 3;

               break;
          }

          // get_struc xi,
          case GET_STRUC:
          {
               // grab f/n
               jbyte mode = code[ip + 1];
               jint xi = (jint)code[ip + 2] + ((mode == STACK_ADDR) ? (ep + 3) : 0);
               jint f_n = *(jint*)(code + ip + 3);

               traceFn1("GET_STRUC", ip, xi, f_n);

               // addr <- deref(xi);
               addr = l3deref(xi);

               // switch STORE[addr]
               //int tmp = data[addr];
               //byte tag = (byte)((tmp & 0xFF000000) >> 24);
               //int a = tmp & 0x00FFFFFF;
               tag = l3state->derefTag;
               a = l3state->derefVal;

               //printf("a = 0x%02x\n", a);

               switch (tag)
               {
                    // case REF:
               case REF:
               {
                    //printf("tag = REF\n");

                    // heap[h] <- STR, h + 1
                    l3state->data[hp] = (STR << 24) | ((hp + 1) & 0xFFFFFF);

                    // heap[h+1] <- f/n
                    l3state->data[hp + 1] = f_n;

                    // bind(addr, h)
                    l3state->data[addr] = (REF << 24) | (hp & 0xFFFFFF);

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

                    if (l3state->data[a] == f_n)
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

               // P <- instruction_size(P)
               ip += 7;

               break;
          }

          // unify_var xi:
          case UNIFY_VAR:
          {
               jbyte mode = code[ip + 1];
               jint xi = (jint)code[ip + 2] + ((mode == STACK_ADDR) ? (ep + 3) : 0);

               trace1("UNIFY_VAR", ip, xi);

               // switch mode
               if (writeMode == JNI_FALSE)
               {
                    // case read:
                    // xi <- heap[s]
                    l3state->data[xi] = l3state->data[sp];
               }
               else
               {
                    // case write:
                    // heap[h] <- REF, h
                    l3state->data[hp] = (REF << 24) | (hp & 0xFFFFFF);

                    // xi <- heap[h]
                    l3state->data[xi] = l3state->data[hp];

                    // h <- h + 1
                    hp++;
               }

               // s <- s + 1
               sp++;

               // P <- P + instruction_size(P)
               ip += 3;

               break;
          }

          // unify_val xi:
          case UNIFY_VAL:
          {
               jbyte mode = code[ip + 1];
               jint xi = (jint)code[ip + 2] + ((mode == STACK_ADDR) ? (ep + 3) : 0);

               trace1("UNIFY_VAL", ip, xi);

               // switch mode
               if (writeMode == JNI_FALSE)
               {
                    // case read:
                    // unify (xi, s)
                    failed = l3unify(xi, sp) == JNI_TRUE ? JNI_FALSE : JNI_TRUE;
               }
               else
               {
                    // case write:
                    // heap[h] <- xi
                    l3state->data[hp] = l3state->data[xi];

                    // h <- h + 1
                    hp++;
               }

               // s <- s + 1
               sp++;

               // P <- P + instruction_size(P)
               ip += 3;

               break;
          }


          // put_var Xn, Ai:
          case PUT_VAR:
          {
               // grab addr, Ai
               jbyte mode = code[ip + 1];
               jint xi = (jint)code[ip + 2] + ((mode == STACK_ADDR) ? (ep + 3) : 0);
               jbyte ai = (jint)code[ip + 3];

               trace2("PUT_VAR", ip, xi, mode, ai, ep);

               // heap[h] <- REF, H
               l3state->data[hp] = (REF << 24) | (hp & 0xFFFFFF);

               // Xn <- heap[h]
               l3state->data[xi] = l3state->data[hp];

               // Ai <- heap[h]
               l3state->data[ai] = l3state->data[hp];

               // h <- h + 1
               hp++;

               // P <- P + instruction_size(P)
               ip += 4;

               break;
          }

          // put_val Xn, Ai:
          case PUT_VAL:
          {
               // grab Ai
               jbyte mode = code[ip + 1];
               jint xi = (jint)code[ip + 2] + ((mode == STACK_ADDR) ? (ep + 3) : 0);
               jbyte ai = (jint)code[ip + 3];

               trace2("PUT_VAL", ip, xi, mode, ai, ep);

               // Ai <- Xn
               l3state->data[ai] = l3state->data[xi];

               // P <- P + instruction_size(P)
               ip += 4;

               break;
          }

          // get var Xn, Ai:
          case GET_VAR:
          {
               // grab Ai
               jbyte mode = code[ip + 1];
               jint xi = (jint)code[ip + 2] + ((mode == STACK_ADDR) ? (ep + 3) : 0);
               jbyte ai = (jint)code[ip + 3];

               trace2("GET_VAR", ip, xi, mode, ai, ep);

               // Xn <- Ai
               l3state->data[xi] = l3state->data[ai];

               // P <- P + instruction_size(P)
               ip += 4;

               break;
          }

          // get_val Xn, Ai:
          case GET_VAL:
          {
               // grab Ai
               jbyte mode = code[ip + 1];
               jint xi = (jint)code[ip + 2] + ((mode == STACK_ADDR) ? (ep + 3) : 0);
               jbyte ai = (jint)code[ip + 3];

               trace2("GET_VAL", ip, xi, mode, ai, ep);

               // unify (Xn, Ai)
               failed = l3unify(xi, ai) == JNI_TRUE ? JNI_FALSE : JNI_TRUE;

               // P <- P + instruction_size(P)
               ip += 4;

               break;
          }

          // call @(p/n)
          case CALL:
          {
               // grab @(p/n) (ip is decremented here, because already took first byte of the address as xi).
               int p_n = *(jint*)(code + ip + 1);

               traceFn0("CALL", ip, p_n);

               // Ensure that the predicate to call is known and linked int, otherwise fail.
               if (p_n == -1)
               {
                    failed = JNI_TRUE;

                    break;
               }

               // CP <- P + instruction_size(P)
               cp = ip + 5;

               // ip <- @(p/n)
               ip = p_n;

               break;
          }

          // proceed:
          case PROCEED:
          {
               trace0("PROCEED", ip);

               // P <- CP
               ip = cp;

               break;
          }

          // allocate N:
          case ALLOCATE:
          {
               // grab N
               int n = (int)code[ip + 1];

               // STACK[newE] <- E
               l3state->data[esp] = ep;

               // STACK[E + 1] <- CP
               l3state->data[esp + 1] = cp;

               // STACK[E + 2] <- N
               l3state->data[esp + 2] = n;

               // E <- newE
               // newE <- E + n + 3
               ep = esp;
               esp = esp + n + 3;

               traceConst("ALLOCATE", ip, n);

               // P <- P + instruction_size(P)
               ip += 2;

               break;
          }

          // deallocate:
          case DEALLOCATE:
          {
               // E <- STACK[E]
               esp = ep;
               ep = l3state->data[ep];

               trace0("DEALLOCATE", ip);

               // P <- STACK[E + 1]
               ip = l3state->data[ep + 1];

               break;
          }

          // An unknown instruction was encountered. Something has gone wrong, so fail.
          default:
          {
               trace0("UNKNOWN (Fail)", ip);

               failed = JNI_TRUE;
               break;
          }
          }
     }

     // Preserve the current state of the machine.
     l3state->hp = hp;
     l3state->sp = sp;
     l3state->cp = cp;
     l3state->ep = ep;
     l3state->esp = esp;
     l3state->writeMode = writeMode;

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
JNIEXPORT jint JNICALL Java_com_thesett_aima_logic_fol_l3_L3ResolvingNativeMachine_deref
(JNIEnv * env, jobject obj, jint a)
{
     return l3deref(a);
}

/*
 * Dereferences an offset from the current environment frame on the stack. Storage slots in the current environment
 * may point to other environment frames, but should not contain unbound variables, so ultimately this dereferencing
 * should resolve onto a structure or variable on the heap.
 *
 * @param  a The offset into the current environment stack frame to dereference.
 *
 * @return The address that the reference refers to.
 */
JNIEXPORT jint JNICALL Java_com_thesett_aima_logic_fol_l3_L3ResolvingNativeMachine_derefStack
(JNIEnv * env, jobject obj, jint a)
{
     return l3deref(a + l3state->ep + 3);
}

/*
 * Gets the heap cell tag for the most recent dereference operation.
 *
 * @return The heap cell tag for the most recent dereference operation.
 */
JNIEXPORT jbyte JNICALL Java_com_thesett_aima_logic_fol_l3_L3ResolvingNativeMachine_getDerefTag
(JNIEnv * env, jobject obj)
{
     return l3state->derefTag;
}

/*
 * Gets the heap cell value for the most recent dereference operation.
 *
 * @return The heap cell value for the most recent dereference operation.
 */
JNIEXPORT jint JNICALL Java_com_thesett_aima_logic_fol_l3_L3ResolvingNativeMachine_getDerefVal
(JNIEnv * env, jobject obj)
{
     return l3state->derefVal;
}

/*
 * Gets the value of the heap cell at the specified location.
 *
 * @param addr The address to fetch from the heap.
 * @return The heap cell at the specified location.
 */
JNIEXPORT jint JNICALL Java_com_thesett_aima_logic_fol_l3_L3ResolvingNativeMachine_getHeap
(JNIEnv * env, jobject obj, jint addr)
{
     return l3state->data[addr];
}
