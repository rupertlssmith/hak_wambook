#include <stdlib.h>
#include <stdio.h>
#include "com_thesett_aima_logic_fol_l0_L0UnifyingNativeMachine.h"

/** Defines the machine instruction types. */
#define PUT_STRUC 0x01
#define SET_VAR 0x02
#define SET_VAL 0x03
#define GET_STRUC 0x04
#define UNIFY_VAR 0x05
#define UNIFY_VAL 0x06

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
} l0MachineState;

l0MachineState *l0state = -1;

/*
 * Creates the machines initial state.
 */
void l0reset()
{
     l0state = malloc(sizeof(l0MachineState));

     // Create fresh heaps, code areas and stacks.
     l0state->heap = malloc((REG_SIZE + HEAP_SIZE) * sizeof(jint));
     l0state->ustack = malloc(USTACK_SIZE * sizeof(jint));

     // Registers are on the top of the heap, so initialize the heap pointers to the heap area.
     l0state->hp = REG_SIZE;
     l0state->sp = REG_SIZE;

     // Clear the unification stack.
     l0state->up = -1;

     // Turn off write mode.
     l0state->writeMode = JNI_FALSE;

     // Could probably not bother resetting these, but will do it anyway just to be sure.
     l0state->derefTag = 0;
     l0state->derefVal = 0;
}

/*
 * Pushes a value onto the unification stack.
 *
 * @param val The value to push onto the stack.
 */
void uPush(jint val)
{
     l0state->ustack[++l0state->up] = val;
}

/*
 * Pops a value from the unification stack.
 *
 * @return The top value from the unification stack.
 */
jint uPop()
{
     return l0state->ustack[l0state->up--];
}

/*
 * Clears the unification stack.
 */
void uClear()
{
     l0state->up = -1;
}

/*
 * Checks if the unification stack is empty.
 *
 * @return <tt>true</tt> if the unification stack is empty, <tt>false</tt> otherwise.
 */
jboolean uEmpty()
{
     return l0state->up == -1 ? JNI_TRUE : JNI_FALSE;
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
jint deref(jint a)
{
     jint addr;
     jint tmp;
     jbyte derefTag;
     jint derefVal;

     //printf("jint deref(jint a = 0x%02x): called\n", a);

     // tag, value <- STORE[a]
     addr = a;
     tmp = l0state->heap[a];
     derefTag = (jbyte)((tmp & 0xFF000000) >> 24);
     derefVal = tmp & 0x00FFFFFF;

     // while tag = REF and value != a
     while ((derefTag == REF))
     {
          // tag, value <- STORE[a]
          addr = derefVal;
          tmp = l0state->heap[derefVal];
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

     l0state->derefTag = derefTag;
     l0state->derefVal = derefVal;

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
jboolean unify(jint a1, jint a2)
{
     jboolean fail;

     //printf("jboolean unify(jint a1 = 0x%02x, jint a2 = 0x%02x): called\n", a1, a2);

     // pdl.push(a1)
     // pdl.push(a2)
     uPush(a1);
     uPush(a2);

     // fail <- false
     fail = JNI_FALSE;

     // while !empty(PDL) and not failed
     while (uEmpty() == JNI_FALSE && !fail)
     {
          // d1 <- deref(pdl.pop())
          // d2 <- deref(pdl.pop())
          // t1, v1 <- STORE[d1]
          // t2, v2 <- STORE[d2]
          jint d1 = deref(uPop());
          jint t1 = l0state->derefTag;
          jint v1 = l0state->derefVal;

          int d2 = deref(uPop());
          int t2 = l0state->derefTag;
          int v2 = l0state->derefVal;

          // if (d1 != d2)
          if (d1 != d2)
          {
               // if (t1 = REF or t2 = REF)
               // bind(d1, d2)
               if ((t1 == REF))
               {
                    l0state->heap[d1] = (REF << 24) | (d2 & 0xFFFFFF);
               }
               else if (t2 == REF)
               {
                    l0state->heap[d2] = (REF << 24) | (d1 & 0xFFFFFF);
               }
               else
               {
                    // f1/n1 <- STORE[v1]
                    // f2/n2 <- STORE[v2]
                    jint f_n1 = l0state->heap[v1];
                    jint f_n2 = l0state->heap[v2];
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
                              uPush(v1 + i);
                              uPush(v2 + i);
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
 * Executes a compiled functor returning an indication of whether or not a unification was found.
 *
 * @param functor The compiled byte code to execute.
 *
 * @return <tt>true</tt> if a unification was found, <tt>false</tt> if the search for one failed.
 */
JNIEXPORT jboolean JNICALL Java_com_thesett_aima_logic_fol_l0_L0UnifyingNativeMachine_execute
(JNIEnv * env, jobject obj, jobject codeBuf)
{
     jint addr;
     jbyte tag;
     jint a;

     jint hp;
     jint sp;
     jboolean writeMode;
     jint ip;

     jboolean failed = JNI_FALSE;

     jsize length = (*env)->GetDirectBufferCapacity(env, codeBuf);
     jbyte * code = (*env)->GetDirectBufferAddress(env, codeBuf);

     // Reset the machine to its initial state if no machine state exists yet.
     if (l0state == -1)
     {
          l0reset();
     }

     hp = l0state->hp;
     sp = l0state->sp;
     writeMode = l0state->writeMode;

     ip = 0;
     uClear();

     //printf("JNIEXPORT jboolean JNICALL Java_com_thesett_aima_logic_fol_l0_L0UnifyingNativeMachine_execute: called\n");

     while (failed == JNI_FALSE && (ip < length))
     {
          // Grab next instruction and switch on it.
          jbyte instruction = code[ip++];
          jint Xi = (jint)code[ip++];

          switch (instruction)
          {
               // put_struc Xi:
          case PUT_STRUC:
          {
               // grab f/n
               jint f_n = *(jint*)(code + ip);
               ip += 4;

               //printf("PUT_STRUC %i,%i (0x%02x)\n", Xi, f_n, f_n);

               // heap[h] <- STR, h + 1
               l0state->heap[hp] = (STR << 24) | ((hp + 1) & 0xFFFFFF);

               // heap[h+1] <- f/n
               l0state->heap[hp + 1] = f_n;

               // Xi <- heap[h]
               l0state->heap[Xi] = l0state->heap[hp];

               // h <- h + 2
               hp += 2;

               break;
          }

          // set_var Xi:
          case SET_VAR:
          {
               //printf("SET_VAR %i\n", Xi);

               // heap[h] <- REF, h
               l0state->heap[hp] = (REF << 24) | (hp & 0xFFFFFF);

               // Xi <- heap[h]
               l0state->heap[Xi] = l0state->heap[hp];

               // h <- h + 1
               hp++;

               break;
          }

          // set_val Xi:
          case SET_VAL:
          {
               //printf("SET_VAL %i\n", Xi);

               // heap[h] <- Xi
               l0state->heap[hp] = l0state->heap[Xi];

               // h <- h + 1
               hp++;

               break;
          }

          // get_struc Xi,
          case GET_STRUC:
          {
               // grab f/n
               jint f_n = *(jint*)(code + ip);
               ip += 4;

               //printf("GET_STRUC %i,%i (0x%02x)\n", Xi, f_n, f_n);

               // addr <- deref(Xi);
               addr = deref(Xi);

               // switch STORE[addr]
               //int tmp = heap[addr];
               //byte tag = (byte)((tmp & 0xFF000000) >> 24);
               //int a = tmp & 0x00FFFFFF;
               tag = l0state->derefTag;
               a = l0state->derefVal;

               //printf("a = 0x%02x\n", a);

               switch (tag)
               {
                    // case REF:
               case REF:
               {
                    //printf("tag = REF\n");

                    // heap[h] <- STR, h + 1
                    l0state->heap[hp] = (STR << 24) | ((hp + 1) & 0xFFFFFF);

                    // heap[h+1] <- f/n
                    l0state->heap[hp + 1] = f_n;

                    // bind(addr, h)
                    l0state->heap[addr] = (REF << 24) | (hp & 0xFFFFFF);

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

                    if (l0state->heap[a] == f_n)
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

          // unify_var Xi:
          case UNIFY_VAR:
          {
               //printf("UNIFY_VAR %i\n", Xi);

               // switch mode
               if (writeMode == JNI_FALSE)
               {
                    // case read:
                    // Xi <- heap[s]
                    l0state->heap[Xi] = l0state->heap[sp];
               }
               else
               {
                    // case write:
                    // heap[h] <- REF, h
                    l0state->heap[hp] = (REF << 24) | (hp & 0xFFFFFF);

                    // Xi <- heap[h]
                    l0state->heap[Xi] = l0state->heap[hp];

                    // h <- h + 1
                    hp++;
               }

               // s <- s + 1
               sp++;

               break;
          }

          // unify_val Xi:
          case UNIFY_VAL:
          {
               //printf("UNIFY_VAL %i\n", Xi);

               // switch mode
               if (writeMode == JNI_FALSE)
               {
                    // case read:
                    // unify (Xi, s)
                    failed = unify(Xi, sp) == JNI_TRUE ? JNI_FALSE : JNI_TRUE;
               }
               else
               {
                    // case write:
                    // heap[h] <- Xi
                    l0state->heap[hp] = l0state->heap[Xi];

                    // h <- h + 1
                    hp++;
               }

               // s <- s + 1
               sp++;

               break;
          }
          }
     }

     // Preserve the current state of the machine.
     l0state->hp = hp;
     l0state->sp = sp;
     l0state->writeMode = writeMode;

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
JNIEXPORT jint JNICALL Java_com_thesett_aima_logic_fol_l0_L0UnifyingNativeMachine_deref
(JNIEnv * env, jobject obj, jint a)
{
     return deref(a);
}

/*
 * Gets the heap cell tag for the most recent dereference operation.
 *
 * @return The heap cell tag for the most recent dereference operation.
 */
JNIEXPORT jbyte JNICALL Java_com_thesett_aima_logic_fol_l0_L0UnifyingNativeMachine_getDerefTag
(JNIEnv * env, jobject obj)
{
     return l0state->derefTag;
}

/*
 * Gets the heap cell value for the most recent dereference operation.
 *
 * @return The heap cell value for the most recent dereference operation.
 */
JNIEXPORT jint JNICALL Java_com_thesett_aima_logic_fol_l0_L0UnifyingNativeMachine_getDerefVal
(JNIEnv * env, jobject obj)
{
     return l0state->derefVal;
}

/*
 * Gets the value of the heap cell at the specified location.
 *
 * @param addr The address to fetch from the heap.
 * @return The heap cell at the specified location.
 */
JNIEXPORT jint JNICALL Java_com_thesett_aima_logic_fol_l0_L0UnifyingNativeMachine_getHeap
(JNIEnv * env, jobject obj, jint addr)
{
     return l0state->heap[addr];
}
