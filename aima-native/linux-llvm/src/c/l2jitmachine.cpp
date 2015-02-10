#include "llvm/BasicBlock.h"
#include "llvm/GlobalVariable.h"
#include "llvm/Module.h"
#include "llvm/Constants.h"
#include "llvm/ADT/STLExtras.h"
#include "llvm/DerivedTypes.h"
#include "llvm/Instructions.h"
#include "llvm/ModuleProvider.h"
#include "llvm/PassManager.h"
#include "llvm/Pass.h"
#include "llvm/LinkAllPasses.h"
#include "llvm/Analysis/Verifier.h"
#include "llvm/Bitcode/ReaderWriter.h"
#include "llvm/Target/TargetData.h"
#include "llvm/Target/TargetSelect.h"
#include "llvm/ExecutionEngine/JIT.h"
#include "llvm/ExecutionEngine/Interpreter.h"
#include "llvm/ExecutionEngine/GenericValue.h"
#include "llvm/Support/IRBuilder.h"
#include "llvm/Support/raw_ostream.h"
#include <iostream>
#include <cstdarg>
#include <cstdlib>
#include <fstream>

#include "com_thesett_aima_logic_fol_l2_L2ResolvingNativeMachine.h"
#include "trace.h"

using namespace llvm;

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
#define STACK_BASE (HEAP_BASE + HEAP_SIZE)

/* Defines the max unification stack depth for the virtual machine. */
#define PDL_SIZE 1000

/* Defines the offset of the base of the PDL in the data area. */
#define PDL_BASE (REG_SIZE + HEAP_SIZE + STACK_SIZE)

/* Defines the highest address in the data area of the virtual machine. */
#define TOP (REG_SIZE + HEAP_SIZE + STACK_SIZE + PDL_SIZE)

#define i32c(n) ConstantInt::get(IntegerType::getInt32Ty(getGlobalContext()), n)
#define i8c(n) ConstantInt::get(IntegerType::getInt8Ty(getGlobalContext()), n)
#define i1c(n) ConstantInt::get(IntegerType::getInt1Ty(getGlobalContext()), n)

typedef struct
{
     /* Holds a module to add compiled code to. */
     Module* M;

     /* Holds the optimization pipeline. */
     PassManager* FPM;

     /* Holds the llvm machines execution environment. */
     ExecutionEngine* EE;

     /* Holds the base of the entire state of the machine. All registers, heaps and stacks are held in here. */
     GlobalVariable* l2MachineState;

     /* Holds the set state function, to set the base pointer to the l2 state vector on initialization. */
     Function* setstateFunction;

     /* Holds the static unify function. At some point this may get custom generated to fit the callers arguments
        and/or inlined. */
     Function* unifyFunction;

     /* Holds the static deref function. At some point this may get custom generated to fit the callers arguments
        and/or inlined. */
     Function* derefFunction;

     /* The Printf function for debugging. */
     Function* thePrintf;

     /* The trace functions. **/
     Function* traceIt;
     Function* trace0;
     Function* trace1;
     Function* trace2;
     Function* traceFn0;
     Function* traceFn1;
     Function* traceConst;

     // Unification stack functions.
} llvmState;

/* Holds the state of the LLVM virtual machine. */
llvmState* vmState;

typedef struct
{
     /* Holds pointer to the base of the heap. */
     int* heapBasePtr;

     /* Holds the primary heap pointer. */
     int hp;

     /* Holds the secondary heap pointer. */
     int sp;

     /* Holds the unification stack pointer. */
     int up;

     /* Holds the environment base pointer. */
     int ep;

     /* Holds the environment, top-of-stack pointer. */
     int esp;

     /* Holds the write mode flag. */
     int wm;
} l2jitMachineState;

/* Holds the state vector of the l2 virtual machine. */
l2jitMachineState* l2state;

int stringId = 0;

/* Holds the heap cell tag from the most recent dereference. */
jbyte derefTag;

/* Holds the heap cell value from the most recent dereference. */
jint derefVal;

void verifyBitCode()
{
     if (verifyModule(*vmState->M))
     {
          std::cerr << "Error: module failed verification.  This shouldn't happen.\n";
          abort();
     }
}

void writeBitCodeToFile()
{
     std::string ErrInfo;
     raw_fd_ostream *out = new raw_fd_ostream("l2.bc", 1, 1, ErrInfo);
     WriteBitcodeToFile(vmState->M, *out);
     out->flush();
     out->close();
}

/*
 * Creates a global variable to hold a string, and makes a call to a 'printf' function to write out trace to the console.
 *
 * @param builder      The IR builder to insert the trace call with.
 * @param module       The module in which to create the global variable.
 * @param text         The trace text, may contain if calling 'printf'
 * @param optionalArgs Varargs to pass to the trace call, should be null ((Value*)0) terminated.
 */
void CreateTrace(IRBuilder<>* builder, Module* mod, const std::string &text, Value* optionalArgs, ...)
{
     // Create unique name for the string constant.
     char* sName = (char*)malloc(12 * sizeof(char));
     sprintf(sName, "string%i", stringId++);

     // Create global variable on the module initialized with the text.
     Constant* msg = ConstantArray::get(mod->getContext(), text);
     /*GlobalVariable* msgGV = new GlobalVariable(mod->getContext(), msg->getType(), true, GlobalValue::InternalLinkage,
                                                msg, sName, mod);*/
     GlobalVariable* msgGV = dyn_cast<GlobalVariable>(mod->getOrInsertGlobal(sName, msg->getType()));
     msgGV->setInitializer(msg);

     // Take a pointer to the message.
     std::vector<Constant*> index(2, Constant::getNullValue(Type::getInt32Ty(mod->getContext())));
     Constant* msgPtr = ConstantExpr::getGetElementPtr(msgGV, &index[0], 2);

     // Build the arguments to the printf function.
     va_list args;
     va_start(args, optionalArgs);

     std::vector<Value*> printfArgs;
     printfArgs.push_back(msgPtr);
     printfArgs.push_back(optionalArgs);
     printfArgs.push_back(optionalArgs);

     while(Value* argV = va_arg(args, Value*))
     {
          printfArgs.push_back(argV);
     }

     va_end(args);

     // Call the printf function with the trace message.
     builder->CreateCall(vmState->thePrintf, printfArgs.begin(), printfArgs.end());// msgPtr);
}

/*
 * Creates a global variable to hold a string, and makes a call to 'printf' to write out trace to the console.
 *
 * @param builder      The IR builder to insert the trace call with.
 * @param module       The module in which to create the global variable.
 * @param traceFn      The trace function to invoke.
 * @param text         The trace text, may contain if calling 'printf'
 * @param optionalArgs Varargs to pass to the trace call, should be null ((Value*)0) terminated.
 */
void CreateTraceFn(IRBuilder<>* builder, Module* mod, Function* traceFn, const std::string &text, Value* optionalArgs, ...)
{
     // Create unique name for the string constant.
     char* sName = (char*)malloc(12 * sizeof(char));
     sprintf(sName, "string%i", stringId++);

     // Create global variable on the module initialized with the text.
     Constant* msg = ConstantArray::get(mod->getContext(), text);
     /*GlobalVariable* msgGV = new GlobalVariable(mod->getContext(), msg->getType(), true, GlobalValue::InternalLinkage,
                                                msg, sName, mod);*/
     GlobalVariable* msgGV = dyn_cast<GlobalVariable>(mod->getOrInsertGlobal(sName, msg->getType()));
     msgGV->setInitializer(msg);

     // Take a pointer to the message.
     std::vector<Constant*> index(2, Constant::getNullValue(Type::getInt32Ty(mod->getContext())));
     Constant* msgPtr = ConstantExpr::getGetElementPtr(msgGV, &index[0], 2);

     // Build the arguments to the printf function.
     va_list args;
     va_start(args, optionalArgs);

     std::vector<Value*> printfArgs;
     printfArgs.push_back(msgPtr);
     printfArgs.push_back(optionalArgs);

     while(Value* argV = va_arg(args, Value*))
     {
          printfArgs.push_back(argV);
     }

     va_end(args);

     // Call the printf function with the trace message.
     builder->CreateCall(traceFn, printfArgs.begin(), printfArgs.end());// msgPtr);
}

/*
 * Creates a getElementPtr instruction at the specified builders current insertion point. Consecutive indexes to the
 * instruction can be specified as varargs, which is not possible through the builder interface.
 *
 * @param builder The builder to insert the instruction into.
 * @param ptr     The varargs indexes to the structural element to obtain a pointer to.
 */
Value* CreateGEPRange(IRBuilder<>* builder, Value* ptr, ...)
{
     va_list args;
     va_start(args, ptr);

     std::vector<Value*> ptrIndices;
     while(Value* argV = va_arg(args, Value*))
     {
          ptrIndices.push_back(argV);
     }

     va_end(args);

     return builder->CreateGEP(ptr, ptrIndices.begin(), ptrIndices.end());
}

/*
 * Creates a getElementPtr instruction, type checking that the CreateGEP method does indeed return that instruction,
 * and casting it appropriately.
 */
Value* CreateGEP(IRBuilder<>* builder, Value *ptr, Value *idx, const Twine &name = "")
{
     return builder->CreateGEP(ptr, idx, name);
}

/*
 * Pushes a value onto the unification stack.
 *
 * @param val The value to push onto the stack.
 */
void l2jitUPush(l2jitMachineState* l2state, jint val)
{
     l2state->heapBasePtr[--(l2state->up)] = val;
}

/*
 * Pops a value from the unification stack.
 *
 * @return The top value from the unification stack.
 */
jint l2jitUPop(l2jitMachineState* l2state)
{
     return l2state->heapBasePtr[(l2state->up)++];
}

/*
 * Clears the unification stack.
 */
void l2jitUClear(l2jitMachineState* l2state)
{
     l2state->up = TOP;
}

/*
 * Checks if the unification stack is empty.
 *
 * @return <tt>true</tt> if the unification stack is empty, <tt>false</tt> otherwise.
 */
jboolean l2jitUEmpty(l2jitMachineState* l2state)
{
     return l2state->up >= TOP ? JNI_TRUE : JNI_FALSE;
}

/*
 * Sets the base pointer to the l2 machine state, to be called on initialization.
 */
extern void l2jitsetstate(l2jitMachineState* l2stateptr)
{
     l2state = l2stateptr;
}

/*
 * Dereferences a heap pointer (or register), returning the address that it refers to after following all
 * reference chains to their conclusion. This method is also side effecting, in that the contents of the
 * refered to heap cell are also loaded into the fields {@link #derefTag} and {@link #derefVal}.
 *
 * @param l2state The address of l2 machine state vector.
 * @param a       The address to dereference, relative to the base of the heap.
 *
 * @return The address that the reference refers to.
 */
extern jint l2jitderef(l2jitMachineState* l2state, jint a)
{
     //std::cout << ("extern jint l2jitderef(jint* heapBasePtr, jint a): called.\n");

     jint addr;
     jint tmp;
     jbyte derefTag;
     jint derefVal;

     // printf("jint l2deref(jint a = 0x%02x): called\n", a);

     // tag, value <- STORE[a]
     addr = a;
     tmp = l2state->heapBasePtr[a];
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
          tmp = l2state->heapBasePtr[derefVal];
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

     //l2state->derefTag = derefTag;
     //l2state->derefVal = derefVal;

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
extern jboolean l2jitunify(l2jitMachineState* l2state, jint a1, jint a2)
{
     jboolean fail;

     // printf("jboolean unify(jint a1 = 0x%02x, jint a2 = 0x%02x): called\n", a1, a2);

     // pdl.push(a1)
     // pdl.push(a2)
     l2jitUPush(l2state, a1);
     l2jitUPush(l2state, a2);

     // fail <- false
     fail = JNI_FALSE;

     // while !empty(PDL) and not failed
     while (l2jitUEmpty(l2state) == JNI_FALSE && !fail)
     {
          // printf("while (l2jitUEmpty(l2state) == JNI_FALSE && !fail)\n");

          // d1 <- deref(pdl.pop(l2state))
          // d2 <- deref(pdl.pop(l2state))
          // t1, v1 <- STORE[d1]
          // t2, v2 <- STORE[d2]
          jint d1 = l2jitderef(l2state, l2jitUPop(l2state));
          jint tmp1 = l2state->heapBasePtr[d1];
          jint t1 = (jbyte)((tmp1 & 0xFF000000) >> 24);
          jint v1 = tmp1 & 0x00FFFFFF;

          jint d2 = l2jitderef(l2state, l2jitUPop(l2state));
          jint tmp2 = l2state->heapBasePtr[d2];
          jint t2 = (jbyte)((tmp2 & 0xFF000000) >> 24);
          jint v2 = tmp2 & 0x00FFFFFF;

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

                    l2state->heapBasePtr[d1] = (REF << 24) | (d2 & 0xFFFFFF);
               }
               else if (t2 == REF)
               {
                    // printf("else if (t2 == REF)\n");

                    l2state->heapBasePtr[d2] = (REF << 24) | (d1 & 0xFFFFFF);
               }
               else
               {
                    // printf("else\n");

                    // printf("v1 = %i\n", v1);
                    // printf("v2 = %i\n", v2);

                    // f1/n1 <- STORE[v1]
                    // f2/n2 <- STORE[v2]
                    jint f_n1 = l2state->heapBasePtr[v1];
                    // printf("f_n1 = %i\n", f_n1);
                    jint f_n2 = l2state->heapBasePtr[v2];
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
                              l2jitUPush(l2state, v1 + i);
                              l2jitUPush(l2state, v2 + i);
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
 * Class:     com_thesett_aima_logic_fol_l2_L2ResolvingNativeMachine
 * Method:    reset
 * Signature: ()V
 *
 * @param env     The native code execution environment.
 * @param obj     The object that is the context to this native method.
 * @param offset  The offset within the code buffer of the byte code to execute.
 */
JNIEXPORT void JNICALL Java_com_thesett_aima_logic_fol_l2_L2ResolvingNativeMachine_nativeReset
(JNIEnv * env, jobject obj)
{
     setbuf(stdout, NULL);

     /*std::cout << "JNIEXPORT void JNICALL Java_com_thesett_aima_logic_fol_l2_L2ResolvingNativeMachine_nativeReset:"
       << "called\n";*/

     // If a previous state of the machine already exists, ensure it is cleaned up before creating a new execution
     // environment.
     if (vmState != 0)
     {
          delete vmState->EE;
     }

     // Allocate space for the machines state.
     vmState = (llvmState*)malloc(sizeof(llvmState));

     // Create the module to add compiled code to.
     vmState->M = new Module("l2machine", getGlobalContext());
     Module* mod = vmState->M;

     // Create the JIT.
     //ExistingModuleProvider* MP = new ExistingModuleProvider(mod);
     //vmState->EE = ExecutionEngine::create(MP, false);
     InitializeNativeTarget();
     vmState->EE = EngineBuilder(mod).create();

     //std::cout << "Created module and execution engine.\n";

     // Set up an optimizing pipeline on the module.
     vmState->FPM = new PassManager();
     vmState->FPM->add(new TargetData(*vmState->EE->getTargetData()));
     vmState->FPM->add(createVerifierPass());

     int optLevel = 0;

     if (optLevel > 0)
     {
          if (optLevel > 1)
          {
               //std::cout << "Adding level 1 optimizations.\n";

               // Clean up disgusting code
               vmState->FPM->add(createCFGSimplificationPass());
               // Remove unused globals
               vmState->FPM->add(createGlobalDCEPass());
               // IP Constant Propagation
               vmState->FPM->add(createIPConstantPropagationPass());
               // Clean up after IPCP
               vmState->FPM->add(createInstructionCombiningPass());
               // Clean up after IPCP
               vmState->FPM->add(createCFGSimplificationPass());
               // Inline small definitions (functions)
               vmState->FPM->add(createFunctionInliningPass());
               // Simplify cfg by copying code
               vmState->FPM->add(createTailDuplicationPass());

               if (optLevel > 2)
               {
                    //std::cout << "Adding level 2 optimizations.\n";

                    // Merge & remove BBs
                    vmState->FPM->add(createCFGSimplificationPass());
                    // Compile silly sequences
                    vmState->FPM->add(createInstructionCombiningPass());
                    // Reassociate expressions
                    vmState->FPM->add(createReassociatePass());
                    // Combine silly seq's
                    vmState->FPM->add(createInstructionCombiningPass());
                    // Eliminate tail calls
                    vmState->FPM->add(createTailCallEliminationPass());
                    // Merge & remove BBs
                    vmState->FPM->add(createCFGSimplificationPass());
                    // Clean up after the unroller
                    vmState->FPM->add(createInstructionCombiningPass());
                    // Clean up after the unroller
                    vmState->FPM->add(createInstructionCombiningPass());
                    // Constant prop with SCCP
                    vmState->FPM->add(createSCCPPass());
               }
               if (optLevel > 3)
               {
                    //std::cout << "Adding level 3 optimizations.\n";

                    // Run instcombine again after redundancy elimination
                    vmState->FPM->add(createInstructionCombiningPass());
                    // Delete dead stores
                    vmState->FPM->add(createDeadStoreEliminationPass());
                    // SSA based 'Aggressive DCE'
                    vmState->FPM->add(createAggressiveDCEPass());
                    // Merge & remove BBs
                    vmState->FPM->add(createCFGSimplificationPass());
                    // Merge dup global constants
                    vmState->FPM->add(createConstantMergePass());
               }
          }

          //std::cout << "Adding level 0 optimizations.\n";

          // Merge & remove BBs
          vmState->FPM->add(createCFGSimplificationPass());
          // Memory To Register
          vmState->FPM->add(createPromoteMemoryToRegisterPass());
          // Compile silly sequences
          vmState->FPM->add(createInstructionCombiningPass());
          // Make sure everything is still good.
          vmState->FPM->add(createVerifierPass());
     }

     // Create the custom type that holds the machines state.
     std::vector<const Type *> l2StateParams;
     l2StateParams.push_back(PointerType::getUnqual(IntegerType::getInt32Ty(mod->getContext()))); // Heap base pointer.
     l2StateParams.push_back(IntegerType::getInt32Ty(mod->getContext()));                   // hp
     l2StateParams.push_back(IntegerType::getInt32Ty(mod->getContext()));                   // sp
     l2StateParams.push_back(IntegerType::getInt32Ty(mod->getContext()));                   // up
     l2StateParams.push_back(IntegerType::getInt32Ty(mod->getContext()));                   // ep
     l2StateParams.push_back(IntegerType::getInt32Ty(mod->getContext()));                   // esp
     l2StateParams.push_back(IntegerType::getInt1Ty(mod->getContext()));                    // Write mode.

     StructType* l2MachineStateStruc = StructType::get(mod->getContext(), l2StateParams, false);
     mod->addTypeName("l2state", l2MachineStateStruc);

     //std::cout << "Created custom type for the machine state.\n";

     // Create an externally linked fprintf to stderr function for outputing debugging trace to.
     std::vector<const Type*> printfParams;
     printfParams.push_back(PointerType::getUnqual(Type::getInt8Ty(mod->getContext())));
     FunctionType* printfType = FunctionType::get(Type::getVoidTy(mod->getContext()), printfParams, true);
     vmState->thePrintf = Function::Create(printfType, GlobalValue::ExternalLinkage, "stderrPrintf", mod);
     vmState->EE->addGlobalMapping(vmState->thePrintf, (void*) &stderrPrintf);

     verifyFunction(*vmState->thePrintf);
     //std::cout << "Created a printf function for debug trace.\n";

     // Create the externally linked trace functions.
     std::vector<const Type*> traceItParams;
     traceItParams.push_back(PointerType::getUnqual(Type::getInt8Ty(mod->getContext())));
     FunctionType* traceItType = FunctionType::get(Type::getVoidTy(mod->getContext()), traceItParams, false);
     vmState->traceIt = Function::Create(traceItType, GlobalValue::ExternalLinkage, "traceIt", mod);
     vmState->EE->addGlobalMapping(vmState->traceIt, (void*) &traceIt);

     std::vector<const Type*> trace0Params;
     trace0Params.push_back(PointerType::getUnqual(Type::getInt8Ty(mod->getContext())));
     trace0Params.push_back(Type::getInt32Ty(mod->getContext()));
     FunctionType* trace0Type = FunctionType::get(Type::getVoidTy(mod->getContext()), trace0Params, false);
     vmState->trace0 = Function::Create(trace0Type, GlobalValue::ExternalLinkage, "trace0", mod);
     vmState->EE->addGlobalMapping(vmState->trace0, (void*) &trace0);

     std::vector<const Type*> trace1Params;
     trace1Params.push_back(PointerType::getUnqual(Type::getInt8Ty(mod->getContext())));
     trace1Params.push_back(Type::getInt32Ty(mod->getContext()));
     trace1Params.push_back(Type::getInt32Ty(mod->getContext()));
     FunctionType* trace1Type = FunctionType::get(Type::getVoidTy(mod->getContext()), trace1Params, false);
     vmState->trace1 = Function::Create(trace1Type, GlobalValue::ExternalLinkage, "trace1", mod);
     vmState->EE->addGlobalMapping(vmState->trace1, (void*) &trace1);

     std::vector<const Type*> trace2Params;
     trace2Params.push_back(PointerType::getUnqual(Type::getInt8Ty(mod->getContext())));
     trace2Params.push_back(Type::getInt32Ty(mod->getContext()));
     trace2Params.push_back(Type::getInt32Ty(mod->getContext()));
     trace2Params.push_back(Type::getInt8Ty(mod->getContext()));
     trace2Params.push_back(Type::getInt32Ty(mod->getContext()));
     trace2Params.push_back(Type::getInt32Ty(mod->getContext()));
     FunctionType* trace2Type = FunctionType::get(Type::getVoidTy(mod->getContext()), trace2Params, false);
     vmState->trace2 = Function::Create(trace2Type, GlobalValue::ExternalLinkage, "trace2", mod);
     vmState->EE->addGlobalMapping(vmState->trace2, (void*) &trace2);

     std::vector<const Type*> traceFn0Params;
     traceFn0Params.push_back(PointerType::getUnqual(Type::getInt8Ty(mod->getContext())));
     traceFn0Params.push_back(Type::getInt32Ty(mod->getContext()));
     traceFn0Params.push_back(Type::getInt32Ty(mod->getContext()));
     FunctionType* traceFn0Type = FunctionType::get(Type::getVoidTy(mod->getContext()), traceFn0Params, false);
     vmState->traceFn0 = Function::Create(traceFn0Type, GlobalValue::ExternalLinkage, "traceFn0", mod);
     vmState->EE->addGlobalMapping(vmState->traceFn0, (void*) &traceFn0);

     std::vector<const Type*> traceFn1Params;
     traceFn1Params.push_back(PointerType::getUnqual(Type::getInt8Ty(mod->getContext())));
     traceFn1Params.push_back(Type::getInt32Ty(mod->getContext()));
     traceFn1Params.push_back(Type::getInt32Ty(mod->getContext()));
     traceFn1Params.push_back(Type::getInt32Ty(mod->getContext()));
     FunctionType* traceFn1Type = FunctionType::get(Type::getVoidTy(mod->getContext()), traceFn1Params, false);
     vmState->traceFn1 = Function::Create(traceFn1Type, GlobalValue::ExternalLinkage, "traceFn1", mod);
     vmState->EE->addGlobalMapping(vmState->traceFn1, (void*) &traceFn1);

     std::vector<const Type*> traceConstParams;
     traceConstParams.push_back(PointerType::getUnqual(Type::getInt8Ty(mod->getContext())));
     traceConstParams.push_back(Type::getInt32Ty(mod->getContext()));
     traceConstParams.push_back(Type::getInt32Ty(mod->getContext()));
     FunctionType* traceConstType = FunctionType::get(Type::getVoidTy(mod->getContext()), traceConstParams, false);
     vmState->traceConst = Function::Create(traceConstType, GlobalValue::ExternalLinkage, "traceConst", mod);
     vmState->EE->addGlobalMapping(vmState->traceConst, (void*) &traceConst);

     // Create an externally linked set state function.
     std::vector<const Type*> setstateParams;
     setstateParams.push_back(PointerType::getUnqual(l2MachineStateStruc));
     FunctionType* setstateType = FunctionType::get(Type::getVoidTy(mod->getContext()), setstateParams, false);
     vmState->setstateFunction = Function::Create(setstateType, GlobalValue::ExternalLinkage, "l2jitsetstate", mod);
     vmState->EE->addGlobalMapping(vmState->setstateFunction, (void*) &l2jitsetstate);

     verifyFunction(*vmState->setstateFunction);
     //std::cout << "Created the set state function.\n";

     // Create the reset method to set up the machines initial state.
     Function *resetFunction =
          cast<Function>(mod->getOrInsertFunction("reset_l2_machine", Type::getVoidTy(mod->getContext()),
                                                         (Type *)0));

     //std::cout << "Opened the reset function.\n";

     // Create a global variable that is a pointer to an instance of the machine state.
     vmState->l2MachineState =
          dyn_cast<GlobalVariable>(mod->getOrInsertGlobal("l2MachineState", PointerType::getUnqual(l2MachineStateStruc)));
     vmState->l2MachineState->setInitializer(Constant::getNullValue(PointerType::getUnqual(l2MachineStateStruc)));

     //std::cout << "Created global variable to point to the machine state.\n";

     // Add the entry block to the new function.
     BasicBlock *bb = BasicBlock::Create(mod->getContext(), "EntryBlock", resetFunction);
     IRBuilder<> builder(bb);
     //CreateTrace(&builder, mod, "reset_l2_machine: called.\n", i32c(0), (Value*)0);

     //std::cout << "Added entry block to reset function.\n";

     // Allocate space for the machines state.
     MallocInst* stateMalloc = builder.CreateMalloc(l2MachineStateStruc, i32c(1));
     Value* statePtrPtr = CreateGEP(&builder, vmState->l2MachineState, i32c(0));
     builder.CreateStore(stateMalloc, statePtrPtr);

     //std::cout << "Added malloc call for machine state.\n";

     // Allocate space for the machines heaps and stacks.
     MallocInst* heapMalloc = builder.CreateMalloc(IntegerType::getInt32Ty(mod->getContext()), i32c(TOP));
     Value* heapPtr = CreateGEPRange(&builder, stateMalloc, i32c(0), i32c(0), (Value*)0);
     builder.CreateStore(heapMalloc, heapPtr);

     //std::cout << "Added malloc call for the machine heap.\n";

     // Set the l2 machine vector base using the set state function.
     Value* statePtr = builder.CreateLoad(statePtrPtr, "statePtr");
     Value *setstateCallParams[] = { statePtr };
     CallInst *failedUnify =
          builder.CreateCall(vmState->setstateFunction, setstateCallParams, array_endof(setstateCallParams));

     // Initialize the machines state.
     Value* hpPtr = CreateGEPRange(&builder, stateMalloc, i32c(0), i32c(1), (Value*)0);
     Value* spPtr = CreateGEPRange(&builder, stateMalloc, i32c(0), i32c(2), (Value*)0);
     Value* upPtr = CreateGEPRange(&builder, stateMalloc, i32c(0), i32c(3), (Value*)0);
     Value* epPtr = CreateGEPRange(&builder, stateMalloc, i32c(0), i32c(4), (Value*)0);
     Value* espPtr = CreateGEPRange(&builder, stateMalloc, i32c(0), i32c(5), (Value*)0);
     Value* wmPtr = CreateGEPRange(&builder, stateMalloc, i32c(0), i32c(6), (Value*)0);
     builder.CreateStore(i32c(REG_SIZE), hpPtr);
     builder.CreateStore(i32c(REG_SIZE), spPtr);
     builder.CreateStore(i32c(TOP), upPtr);
     builder.CreateStore(i32c(STACK_BASE), epPtr);
     builder.CreateStore(i32c(STACK_BASE), espPtr);
     builder.CreateStore(i1c(0), wmPtr);

     //std::cout << "Created initialisations for machine registers.\n";

     builder.CreateRetVoid();

     verifyFunction(*resetFunction);
     //std::cout << "Finished creating the reset function.\n";

     // Create an externally linked deref function.
     std::vector<const Type*> derefParams;
     derefParams.push_back(PointerType::getUnqual(l2MachineStateStruc));
     derefParams.push_back(IntegerType::getInt32Ty(mod->getContext()));
     FunctionType* derefType = FunctionType::get(IntegerType::getInt32Ty(mod->getContext()), derefParams, false);
     vmState->derefFunction = Function::Create(derefType, GlobalValue::ExternalLinkage, "l2jitderef", mod);
     vmState->EE->addGlobalMapping(vmState->derefFunction, (void*) &l2jitderef);

     verifyFunction(*vmState->derefFunction);
     //std::cout << "Created the deref function.\n";

     // Create an externally linked unify function.
     std::vector<const Type*> unifyParams;
     unifyParams.push_back(PointerType::getUnqual(l2MachineStateStruc));
     unifyParams.push_back(IntegerType::getInt32Ty(mod->getContext()));
     unifyParams.push_back(IntegerType::getInt32Ty(mod->getContext()));
     FunctionType* unifyType = FunctionType::get(IntegerType::getInt1Ty(mod->getContext()), unifyParams, false);
     vmState->unifyFunction = Function::Create(unifyType, GlobalValue::ExternalLinkage, "l2jitunify", mod);
     vmState->EE->addGlobalMapping(vmState->unifyFunction, (void*) &l2jitunify);

     verifyFunction(*vmState->unifyFunction);
     //std::cout << "Created the unify function.\n";

     // Provide debug dump of everything created for the reset.
     //mod->print(std::cout);
     //std::cout << *vmState->l2MachineState;
     //std::cout << *vmState->unifyFunction;
     //std::cout << *vmState->derefFunction;
     //std::cout << *vmState->thePrintf;
     //std::cout << *resetFunction;

     //std::cout << "Reset function completed.\n";

     verifyBitCode();
     writeBitCodeToFile();

     // Execute the reset method immediately.
     vmState->EE->runFunction(resetFunction, std::vector<GenericValue>());
}

/*
 * Creates a marker cell for addition to the heap. There are two types of marker cells 'STR' which is followed by a
 * heap pointer to a structure, and 'REF' which is followed by a heap pointer to another marker cell.
 *
 * <p/>The marker type value will be placed in the top eight bits of the heap cell, and the offset in the remaining
 * 24. The marker cell value will be equal to:
 *
 * <p/><pre>
 * (cellType << 24) | ((hp + offset) & 0xFFFFFF)
 * </pre>
 *
 * @param builder     The LLVM builder helper positioned to insert the instructions.
 * @param hp          The current value of 'hp' where the cell will be stored.
 * @param offset      The offset relative to 'hp' that the cell refers to.
 * @param cellType    The cell marker type value.
 *
 * @return The value to store in the heap cell, for the specified structure or reference.
 */
Value* createHeapMarkerCell(IRBuilder<> &builder, Value* hp, Value* offset, int cellType)
{
     Value* hpIncr = builder.CreateAdd(hp, offset);
     Value* hpIncrMask = builder.CreateAnd(hpIncr, i32c(0xFFFFFF));
     Value* result = builder.CreateOr(i32c(cellType << 24), hpIncrMask);

     //CreateTrace(&builder, vmState->M, "offset = %x\n" , offset, (Value*)0);
     //CreateTrace(&builder, vmState->M, "masked = %x\n" , hpIncrMask, (Value*)0);
     //CreateTrace(&builder, vmState->M, "result = %x\n" , result, (Value*)0);

     return result;
}

/*
 * Loads a heap offset from the machines state, and creats a heap pointer within the heap to the cell with that offset.
 *
 * @param sptr    A pointer within the machine state, where the heap offset is stored.
 * @param baseptr A pointer to the base of the heap.
 *
 * @param optr    The name of the variable to hold the heap cell pointer.
 * @param p       The name of the variable to hold the heap offset.
 */
#define loadState(sptr, baseptr, optr, p)           \
     Value* p = builder.CreateLoad(sptr, p);        \
     Value* optr = CreateGEP(&builder, baseptr, p);

/*
 * Adds the specified increment to a heap offset, and stores the result back into the machines state.
 *
 * @param builder     The LLVM builder helper positioned to insert the instructions.
 * @param heapBasePtr A pointer to the base of the heap.
 * @param hp          The current value of the heap offset to adjust.
 * @param inc         The increment to add to the heap offset.
 * @param hpPtr       A pointer to the location in the machine state where the heap offset is stored.
 */
void updateHeapOffset(IRBuilder<> &builder, Value* hp, Value* inc, Value* hpPtr)
{
     Value *newHp = builder.CreateAdd(hp, inc);
     builder.CreateStore(newHp, hpPtr);
}

/*
 * Creates code and returns a pointer, to either a register or local variable, depending
 * on the addressing mode.
 *
 * @param builder     The LLVM builder helper positioned to insert the instructions.
 * @param xi          The register or local variable offset.
 * @param mode        The addressing mode (REG_ADDR or STACK_ADDR).
 * @param regBasePtr  A pointer to the base of the register file.
 * @param heapBasePtr A pointer to the base of the entire l2 machine heap.
 * @param epPtr       A pointer to the base of the current environment frame.
 */
Value* getRegOrArgPtr(IRBuilder<> &builder, jint xi, jbyte mode, Value* regBasePtr, Value* heapBasePtr, Value* epPtr)
{
     Value* resultPtr;

     if (mode == REG_ADDR)
     {
          resultPtr = CreateGEP(&builder, regBasePtr, i32c(xi));
     }
     else if (mode == STACK_ADDR)
     {
          // Offset the argument index by 2, as the first two slots of the environment
          // contain the environment size, and a previous environment pointer.
          loadState(epPtr, heapBasePtr, envPtr, ep);
          resultPtr = CreateGEP(&builder, envPtr, i32c(xi + 2));
     }

     return resultPtr;
}

/*
 * Creates code and returns an offset, relative to the base of the machine heap, to either a register
 * or local variable, depending on the addressing mode.
 *
 * @param builder     The LLVM builder helper positioned to insert the instructions.
 * @param xi          The register or local variable offset.
 * @param mode        The addressing mode (REG_ADDR or STACK_ADDR).
 * @param regBasePtr  A pointer to the base of the register file.
 * @param heapBasePtr A pointer to the base of the entire l2 machine heap.
 * @param epPtr       A pointer to the base of the current environment frame.
 */
Value* getRegOrArgOffset(IRBuilder<> &builder, jint xi, jbyte mode, Value* regBasePtr, Value* heapBasePtr, Value* epPtr)
{
     Value* result;

     if (mode == REG_ADDR)
     {
          result = i32c(xi);
     }
     else if (mode == STACK_ADDR)
     {
          // Offset the argument index by 2, as the first two slots of the environment
          // contain the environment size, and a previous environment pointer.
          loadState(epPtr, heapBasePtr, envPtr, ep);

          result = builder.CreateAdd(ep, i32c(xi + 2));
     }

     return result;
}

/*
 * Notified whenever code is added to the machine. This provides a hook in point at which the machine may,
 * if required, compile the code down below the byte code level.
 *
 * @param codeBuffer The code buffer.
 * @param codeOffset The start offset of the new code.
 * @param length     The length of the new code.
 */
JNIEXPORT void JNICALL Java_com_thesett_aima_logic_fol_l2_L2ResolvingNativeMachine_codeAdded
(JNIEnv * env, jobject object, jobject codeBuf, jint offset, jint length)
{
     /*std::cout << "\nJNIEXPORT void JNICALL Java_com_thesett_aima_logic_fol_l2_L2ResolvingNativeMachine_codeAdded: "
       << "called\n";*/
     //std::cout << "\nCompilation Trace.\n";

     jboolean stopCompilation = JNI_FALSE;
     jbyte* code = (jbyte*)env->GetDirectBufferAddress(codeBuf);
     jint ip = offset;
     jint ep = 0;
     Module* mod = vmState->M;

     // Create a function to hold the results of compiling down the byte code.
     char* fName = (char*)malloc(100 * sizeof(char));
     sprintf(fName, "f_%i", offset);
     //std::cout << "Creating function: " << fName << "\n";

     Function *newFunction =
          cast<Function>(mod->getOrInsertFunction(fName, Type::getInt32Ty(mod->getContext()), (Type *)0));

     // Add the entry block to the new function.
     BasicBlock *bb = BasicBlock::Create(mod->getContext(), "EntryBlock", newFunction);
     IRBuilder<> builder(bb);

     //char* calledTraceMsg = (char*)malloc(200 * sizeof(char));
     //sprintf(calledTraceMsg, "%s: called.\n", fName);
     //CreateTrace(&builder, mod, calledTraceMsg, i32c(0), (Value*)0);
     //free(calledTraceMsg);

     //std::cout << "Added entry block to the function.\n";

     // Take pointers to the base of the heap and the other elements of the machines state.
     Value* statePtrPtr = CreateGEP(&builder, vmState->l2MachineState, i32c(0), "statePtrPtr");
     Value* statePtr = builder.CreateLoad(statePtrPtr, "statePtr");
     Value* hpPtr = CreateGEPRange(&builder, statePtr, i32c(0), i32c(1), (Value*)0);
     Value* spPtr = CreateGEPRange(&builder, statePtr, i32c(0), i32c(2), (Value*)0);
     Value* upPtr = CreateGEPRange(&builder, statePtr, i32c(0), i32c(3), (Value*)0);
     Value* epPtr = CreateGEPRange(&builder, statePtr, i32c(0), i32c(4), (Value*)0);
     Value* espPtr = CreateGEPRange(&builder, statePtr, i32c(0), i32c(5), (Value*)0);
     Value* wmPtr = CreateGEPRange(&builder, statePtr, i32c(0), i32c(6), (Value*)0);

     //std::cout << "Took pointers into the machine state.\n";

     // Load the heap pointers and set up a base pointer to the various data sections.
     Value* heapPtrPtr = CreateGEPRange(&builder, statePtr, i32c(0), i32c(0), (Value*)0);
     Value* heapBasePtr = builder.CreateLoad(heapPtrPtr, "heapBasePtr");
     Value* regBasePtr = CreateGEP(&builder, heapBasePtr, i32c(0), "regBasePtr");

     //std::cout << "Took pointers into the machine heap.\n";

     //std::cout << "Looping over instructions from " << ip << " to " << (offset + length) << ".\n";

     // Loop over the inserted instructions compiling them down into LLVM byte code instructions.
     while (ip < (offset + length) && !stopCompilation)
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
               jint xi = (jint)code[ip + 2];
               Value* regXiPtr = getRegOrArgPtr(builder, xi, mode, regBasePtr, heapBasePtr, epPtr);
               jint f_n = *(jint*)(code + ip + 3);

               //traceFn1("PUT_STRUC", ip, xi, f_n);
               CreateTraceFn(&builder, mod, vmState->traceFn1, "PUT_STRUC", i32c(ip), i32c(xi), i32c(f_n), (Value*)0);
               //CreateTrace(&builder, mod, "hello %i\n", i32c(ip), (Value*)0);
               //CreateTrace(&builder, mod, "hello %i\n", i32c(50), (Value*)0);

               loadState(hpPtr, heapBasePtr, heapPtr, hp);

               // heap[h] <- STR, h + 1
               Value* toHeap = createHeapMarkerCell(builder, hp, i32c(1), STR);
               builder.CreateStore(toHeap, heapPtr);

               // heap[h+1] <- f/n
               Value* heapPtrInc = CreateGEP(&builder, heapPtr, i32c(1));
               builder.CreateStore(i32c(f_n), heapPtrInc);

               // xi <- heap[h]
               Value *toreg = builder.CreateLoad(heapPtr);
               builder.CreateStore(toreg, regXiPtr);

               // h <- h + 2
               updateHeapOffset(builder, hp, i32c(2), hpPtr);

               // P <- instruction_size(P)
               ip += 7;

               break;
          }

          // set_var xi:
          case SET_VAR:
          {
               jbyte mode = code[ip + 1];
               jint xi = (jint)code[ip + 2];
               Value* regXiPtr = getRegOrArgPtr(builder, xi, mode, regBasePtr, heapBasePtr, epPtr);

               //trace1("SET_VAR", ip, xi);
               CreateTraceFn(&builder, mod, vmState->trace1, "SET_VAR", i32c(ip), i32c(xi), (Value*)0);

               loadState(hpPtr, heapBasePtr, heapPtr, hp);

               // heap[h] <- REF, h
               Value* toHeap = createHeapMarkerCell(builder, hp, i32c(0), REF);
               builder.CreateStore(toHeap, heapPtr);

               // xi <- heap[h]
               Value *toreg = builder.CreateLoad(heapPtr);
               builder.CreateStore(toreg, regXiPtr);

               // h <- h + 1
               updateHeapOffset(builder, hp, i32c(1), hpPtr);

               // P <- instruction_size(P)
               ip += 3;

               break;
          }

          // set_val xi:
          case SET_VAL:
          {
               jbyte mode = code[ip + 1];
               jint xi = (jint)code[ip + 2];
               Value* regXiPtr = getRegOrArgPtr(builder, xi, mode, regBasePtr, heapBasePtr, epPtr);

               //trace1("SET_VAL", ip, xi);
               CreateTraceFn(&builder, mod, vmState->trace1, "SET_VAL", i32c(ip), i32c(xi), (Value*)0);

               loadState(hpPtr, heapBasePtr, heapPtr, hp);

               // heap[h] <- xi
               Value *toheap = builder.CreateLoad(regXiPtr);
               builder.CreateStore(toheap, heapPtr);

               // h <- h + 1
               updateHeapOffset(builder, hp, i32c(1), hpPtr);

               // P <- instruction_size(P)
               ip += 3;

               break;
          }

          // get_struc xi,
          case GET_STRUC:
          {
               // grab f/n
               jbyte mode = code[ip + 1];
               jint xi = (jint)code[ip + 2];
               jint f_n = *(jint*)(code + ip + 3);
               Value* regXiOffset = getRegOrArgOffset(builder, xi, mode, regBasePtr, heapBasePtr, epPtr);

               //traceFn1("GET_STRUC", ip, xi, f_n);
               CreateTraceFn(&builder, mod, vmState->traceFn1, "GET_STRUC", i32c(ip), regXiOffset, i32c(f_n), (Value*)0);

               // addr <- deref(xi);
               Value *derefParams[] = {
                    statePtr,
                    regXiOffset
               };

               CallInst* addr =
                    builder.CreateCall<>(vmState->derefFunction, derefParams, array_endof(derefParams), "addr");
               Value* heapAddrPtr = CreateGEP(&builder, heapBasePtr, addr, "heapAddrPtr");
               Value* heapVal = builder.CreateLoad(heapAddrPtr, "heapVal");
               Value* tag = builder.CreateLShr(heapVal, i32c(24), "tag");
               Value* val = builder.CreateAnd(heapVal, i32c(0x00FFFFFF), "val");

               CreateTrace(&builder, mod, "addr    = %i\n", addr, (Value*)0);
               CreateTrace(&builder, mod, "heapVal = %i\n", heapVal, (Value*)0);
               CreateTrace(&builder, mod, "tag     = %i\n", tag, (Value*)0);
               CreateTrace(&builder, mod, "val     = %i\n", val, (Value*)0);

               // switch STORE[addr]
               BasicBlock *tagRefBlock = BasicBlock::Create(mod->getContext(), "getStrucRefTag", newFunction);
               BasicBlock *tagStrBlock = BasicBlock::Create(mod->getContext(), "getStrucStrTag", newFunction);
               BasicBlock *continueBlock = BasicBlock::Create(mod->getContext(), "getStrucTagContinue", newFunction);

               Value *isRef = builder.CreateICmpEQ(tag, i32c(REF), "isRef");
               builder.CreateCondBr(isRef, tagRefBlock, tagStrBlock);

               // case REF:
               {
                    builder.SetInsertPoint(tagRefBlock);

                    loadState(hpPtr, heapBasePtr, heapPtr, hp);

                    // heap[h] <- STR, h + 1
                    Value* toHeap = createHeapMarkerCell(builder, hp, i32c(1), STR);
                    builder.CreateStore(toHeap, heapPtr, "heapPtr");

                    // heap[h+1] <- f/n
                    Value* heapPtrInc = CreateGEP(&builder, heapPtr, i32c(1), "heapPtrInc");
                    builder.CreateStore(i32c(f_n), heapPtrInc);

                    // bind(addr, h)
                    Value* toHeapAddr = createHeapMarkerCell(builder, hp, i32c(0), REF);
                    builder.CreateStore(toHeapAddr, heapAddrPtr);

                    // h <- h + 2
                    updateHeapOffset(builder, hp, i32c(2), hpPtr);

                    // mode <- write
                    builder.CreateStore(i1c(1), wmPtr);

                    builder.CreateBr(continueBlock);
               }

               // case STR, a:
               {
                    builder.SetInsertPoint(tagStrBlock);

                    // switch heap[a]
                    BasicBlock *matchTrueBlock =
                         BasicBlock::Create(mod->getContext(), "getStrucMatchTrue", newFunction);
                    BasicBlock *matchFalseBlock =
                         BasicBlock::Create(mod->getContext(), "getStrucMatchFalse", newFunction);
                    BasicBlock *matchContinueBlock =
                         BasicBlock::Create(mod->getContext(), "getStrucMatchContinue", newFunction);

                    Value* derefStrPtr = CreateGEP(&builder, heapBasePtr, val, "derefStrPtr");
                    Value* strCmp = builder.CreateLoad(derefStrPtr);

                    CreateTrace(&builder, mod, "f_n     = %i\n", i32c(f_n), (Value*)0);
                    CreateTrace(&builder, mod, "strCmp  = %i\n", strCmp, (Value*)0);

                    Value* isMatch = builder.CreateICmpEQ(strCmp, i32c(f_n), "isMatch");
                    builder.CreateCondBr(isMatch, matchTrueBlock, matchFalseBlock);

                    // if heap[a] = f/n
                    {
                         builder.SetInsertPoint(matchTrueBlock);
                         CreateTrace(&builder, mod, "In matchTrueBlock.\n", i32c(0), (Value*)0);

                         // s <- a + 1
                         Value *newSp = builder.CreateAdd(val, i32c(1));
                         builder.CreateStore(newSp, spPtr);

                         // mode <- read
                         builder.CreateStore(i1c(0), wmPtr);

                         builder.CreateBr(matchContinueBlock);
                    }

                    // else
                    {
                         builder.SetInsertPoint(matchFalseBlock);
                         CreateTrace(&builder, mod, "In matchFalseBlock.\n", i32c(0), (Value*)0);

                         // fail
                         CreateTrace(&builder, mod, "Failed on GET_STRUC.\n", i32c(0), (Value*)0);
                         builder.CreateRet(i32c(0));
                    }

                    builder.SetInsertPoint(matchContinueBlock);
                    builder.CreateBr(continueBlock);
               }

               builder.SetInsertPoint(continueBlock);

               // P <- instruction_size(P)
               ip += 7;

               break;
          }

          // unify_var xi:
          case UNIFY_VAR:
          {
               jbyte mode = code[ip + 1];
               jint xi = (jint)code[ip + 2];
               Value* regXiPtr = getRegOrArgPtr(builder, xi, mode, regBasePtr, heapBasePtr, epPtr);

               //trace1("UNIFY_VAR", ip, xi);
               CreateTraceFn(&builder, mod, vmState->trace1, "UNIFY_VAR", i32c(ip), i32c(xi), (Value*)0);

               loadState(hpPtr, heapBasePtr, heapPtr, hp);
               loadState(spPtr, heapBasePtr, heapSpPtr, sp);

               // switch write mode
               BasicBlock *writeModeFalseBlock =
                    BasicBlock::Create(mod->getContext(), "unifyVarWriteModeFalse", newFunction);
               BasicBlock *writeModeTrueBlock =
                    BasicBlock::Create(mod->getContext(), "unifyVarWriteModeTrue", newFunction);
               BasicBlock *continueBlock = BasicBlock::Create(mod->getContext(), "unifyVarWriteModeContinue", newFunction);

               Value *writeMode = builder.CreateLoad(wmPtr);
               builder.CreateCondBr(writeMode, writeModeTrueBlock, writeModeFalseBlock);

               // case read:
               {
                    builder.SetInsertPoint(writeModeFalseBlock);

                    // xi <- heap[s]
                    Value *toreg = builder.CreateLoad(heapSpPtr);
                    builder.CreateStore(toreg, regXiPtr);

                    builder.CreateBr(continueBlock);
               }

               // case write:
               {
                    builder.SetInsertPoint(writeModeTrueBlock);

                    // heap[h] <- REF, h
                    Value* toHeap = createHeapMarkerCell(builder, hp, i32c(0), REF);
                    builder.CreateStore(toHeap, heapPtr);

                    // xi <- heap[h]
                    Value *toreg = builder.CreateLoad(heapPtr);
                    builder.CreateStore(toreg, regXiPtr);

                    builder.CreateBr(continueBlock);
               }

               builder.SetInsertPoint(continueBlock);

               // h <- h + 1
               updateHeapOffset(builder, hp, i32c(1), hpPtr);

               // s <- s + 1
               updateHeapOffset(builder, sp, i32c(1), spPtr);

               // P <- P + instruction_size(P)
               ip += 3;

               break;
          }

          // unify_val xi:
          case UNIFY_VAL:
          {
               jbyte mode = code[ip + 1];
               jint xi = (jint)code[ip + 2];
               Value* regXiPtr = getRegOrArgPtr(builder, xi, mode, regBasePtr, heapBasePtr, epPtr);

               //trace1("UNIFY_VAL", ip, xi);
               CreateTraceFn(&builder, mod, vmState->trace1, "UNIFY_VAL", i32c(ip), i32c(xi), (Value*)0);

               loadState(spPtr, heapBasePtr, heapSpPtr, sp);

               // switch write mode
               BasicBlock *writeModeFalseBlock =
                    BasicBlock::Create(mod->getContext(), "unifyValWriteModeFalse", newFunction);
               BasicBlock *writeModeTrueBlock =
                    BasicBlock::Create(mod->getContext(), "unifyValWriteModeTrue", newFunction);
               BasicBlock *continueBlock = BasicBlock::Create(mod->getContext(), "unifyValWriteModeContinue", newFunction);

               Value *writeMode = builder.CreateLoad(wmPtr, "writeMode");
               builder.CreateCondBr(writeMode, writeModeTrueBlock, writeModeFalseBlock);

               // case read:
               {
                    builder.SetInsertPoint(writeModeFalseBlock);

                    // unify (xi, s)
                    Value *unifyParams[] = {
                         statePtr,
                         i32c(xi),
                         sp
                    };

                    CallInst *failedUnify =
                         builder.CreateCall(vmState->unifyFunction, unifyParams, array_endof(unifyParams));

                    // check if unification failed.
                    BasicBlock *failBlock = BasicBlock::Create(mod->getContext(), "unifyValUnifyFailed", newFunction);

                    Value *failed = builder.CreateICmpEQ(failedUnify, i1c(0));
                    //builder.CreateCondBr(failedUnify, failBlock, continueBlock);
                    builder.CreateCondBr(failedUnify, continueBlock, failBlock);

                    // fail case:
                    {
                         builder.SetInsertPoint(failBlock);
                         builder.CreateRet(i32c(0));
                    }
               }

               // case write:
               {
                    builder.SetInsertPoint(writeModeTrueBlock);

                    loadState(hpPtr, heapBasePtr, heapPtr, hp);

                    // heap[h] <- xi
                    Value *toheap = builder.CreateLoad(regXiPtr);
                    builder.CreateStore(toheap, heapPtr);

                    // h <- h + 1
                    updateHeapOffset(builder, hp, i32c(1), hpPtr);

                    builder.CreateBr(continueBlock);
               }

               builder.SetInsertPoint(continueBlock);

               // s <- s + 1
               updateHeapOffset(builder, sp, i32c(1), spPtr);

               // P <- P + instruction_size(P)
               ip += 3;

               break;
          }


          // put_var Xn, Ai:
          case PUT_VAR:
          {
               // grab addr, Ai
               jbyte mode = code[ip + 1];
               jint xi = (jint)code[ip + 2];
               Value* regXiPtr = getRegOrArgPtr(builder, xi, mode, regBasePtr, heapBasePtr, epPtr);
               jbyte ai = (jint)code[ip + 3];

               //trace2("PUT_VAR", ip, xi, mode, ai, -3);
               CreateTraceFn(&builder, mod, vmState->trace2, "PUT_VAR", i32c(ip), i32c(xi), i8c(mode), i32c(ai), i32c(-3), (Value*)0);

               loadState(hpPtr, heapBasePtr, heapPtr, hp);

               // heap[h] <- REF, h
               Value* toHeap = createHeapMarkerCell(builder, hp, i32c(0), REF);
               builder.CreateStore(toHeap, heapPtr);

               // Xn <- heap[h]
               Value *toreg = builder.CreateLoad(heapPtr);
               builder.CreateStore(toreg, regXiPtr);

               // Ai <- heap[h]
               Value* regAiPtr = CreateGEP(&builder, regBasePtr, i32c(xi));
               builder.CreateStore(toreg, regAiPtr);

               // h <- h + 1
               updateHeapOffset(builder, hp, i32c(1), hpPtr);

               // P <- P + instruction_size(P)
               ip += 4;

               break;
          }

          // put_val Xn, Ai:
          case PUT_VAL:
          {
               // grab Ai
               jbyte mode = code[ip + 1];
               jint xi = (jint)code[ip + 2];
               Value* regXiPtr = getRegOrArgPtr(builder, xi, mode, regBasePtr, heapBasePtr, epPtr);
               jbyte ai = (jint)code[ip + 3];

               //trace2("PUT_VAL", ip, xi, mode, ai, -3);
               CreateTraceFn(&builder, mod, vmState->trace2, "PUT_VAL", i32c(ip), i32c(xi), i8c(mode), i32c(ai), i32c(-3), (Value*)0);

               // Ai <- Xn
               Value* regAiPtr = CreateGEP(&builder, regBasePtr, i32c(ai));
               Value* toMove = builder.CreateLoad(regXiPtr);
               CreateTrace(&builder, mod, "toMove = %x\n", toMove, (Value*)0);
               builder.CreateStore(toMove, regAiPtr);

               // P <- P + instruction_size(P)
               ip += 4;

               break;
          }

          // get var Xn, Ai:
          case GET_VAR:
          {
               // grab Ai
               jbyte mode = code[ip + 1];
               jint xi = (jint)code[ip + 2];
               Value* regXiPtr = getRegOrArgPtr(builder, xi, mode, regBasePtr, heapBasePtr, epPtr);
               jbyte ai = (jint)code[ip + 3];

               //trace2("GET_VAR", ip, xi, mode, ai, -3);
               CreateTraceFn(&builder, mod, vmState->trace2, "GET_VAR", i32c(ip), i32c(xi), i8c(mode), i32c(ai), i32c(-3), (Value*)0);

               // Xn <- Ai
               Value* regAiPtr = CreateGEP(&builder, regBasePtr, i32c(ai));
               Value* toMove = builder.CreateLoad(regAiPtr);
               builder.CreateStore(toMove, regXiPtr);

               // P <- P + instruction_size(P)
               ip += 4;

               break;
          }

          // get_val Xn, Ai:
          case GET_VAL:
          {
               // grab Ai
               jbyte mode = code[ip + 1];
               jint xi = (jint)code[ip + 2];
               jbyte ai = (jint)code[ip + 3];
               Value* regXiOffset = getRegOrArgOffset(builder, xi, mode, regBasePtr, heapBasePtr, epPtr);

               //trace2("GET_VAL", ip, xi, mode, ai, -3);
               CreateTraceFn(&builder, mod, vmState->trace2, "GET_VAL", i32c(ip), regXiOffset, i8c(mode), i32c(ai), i32c(-3), (Value*)0);

               // unify (Xn, Ai)
               Value *unifyParams[] = {
                    statePtr,
                    regXiOffset,
                    i32c(ai)
               };

               CallInst *failedUnify =
                    builder.CreateCall(vmState->unifyFunction, unifyParams, array_endof(unifyParams));

               // check if unification failed.
               BasicBlock *failBlock = BasicBlock::Create(mod->getContext(), "getValUnifyFailed", newFunction);
               BasicBlock *continueBlock = BasicBlock::Create(mod->getContext(), "getValUnifyContinue", newFunction);

               Value *failed = builder.CreateICmpEQ(failedUnify, i1c(0));
               //builder.CreateCondBr(failedUnify, failBlock, continueBlock);
               builder.CreateCondBr(failedUnify, continueBlock, failBlock);

               // fail case:
               {
                    builder.SetInsertPoint(failBlock);

                    //CreateTrace(&builder, mod, "Failed unify on GET_VAL.\n", i32c(0), (Value*)0);
                    builder.CreateRet(i32c(0));
               }

               builder.SetInsertPoint(continueBlock);

               // P <- P + instruction_size(P)
               ip += 4;

               break;
          }

          // call @(p/n)
          case CALL:
          {
               // grab @(p/n) (ip is decremented here, because already took first byte of the address as xi).
               int p_n = *(jint*)(code + ip + 1);

               //traceFn0("CALL", ip, p_n);
               CreateTraceFn(&builder, mod, vmState->traceFn0, "CALL", i32c(ip), i32c(p_n), (Value*)0);

               // Ensure that the predicate to call is known and linked in, otherwise fail the compilation.
               if (p_n == -1)
               {
                    // fail at runtime.
                    builder.CreateRet(i32c(0));

                    // stop compilation.
                    stopCompilation = JNI_TRUE;;
               }
               else
               {
                    // CP <- P + instruction_size(P)
                    // ip <- @(p/n)

                    // Obtain the address of the compiled code to call.
                    char* cfName = (char*)malloc(30 * sizeof(char));
                    sprintf(cfName, "f_%i", p_n);
                    Function* callTarget = vmState->EE->FindFunctionNamed(cfName);
                    free(cfName);

                    // Call the compiled query.
                    CallInst* failedCall = builder.CreateCall(callTarget);

                    // Check if the call failed.
                    BasicBlock* failBlock = BasicBlock::Create(mod->getContext(), "callFailed", newFunction);
                    BasicBlock* continueBlock = BasicBlock::Create(mod->getContext(), "callContinue", newFunction);

                    Value* failed = builder.CreateICmpEQ(failedCall, i32c(0));
                    //CreateTrace(&builder, mod, "failedCall = %i\n", failedCall, (Value*)0);
                    builder.CreateCondBr(failed, failBlock, continueBlock);

                    // Fail case:
                    {
                         builder.SetInsertPoint(failBlock);
                         //CreateTrace(&builder, mod, "Call failed.\n", i32c(0), (Value*)0);
                         builder.CreateRet(i32c(0));
                    }

                    builder.SetInsertPoint(continueBlock);
                    //CreateTrace(&builder, mod, "Call succeeded.\n", i32c(0), (Value*)0);
               }

               // P <- P + instruction_Size(P)
               ip += 5;

               break;
          }

          // proceed:
          case PROCEED:
          {
               //trace0("PROCEED", ip);
               CreateTraceFn(&builder, mod, vmState->trace0, "PROCEED", i32c(ip), (Value*)0);

               // P <- CP
               builder.CreateRet(i32c(1));

               // P <- P + instruction_Size(P)
               ip += 1;

               break;
          }

          // allocate N:
          case ALLOCATE:
          {
               // grab N
               int n = (int)code[ip + 1];

               //traceConst("ALLOCATE", ip, n);
               CreateTraceFn(&builder, mod, vmState->traceConst, "ALLOCATE", i32c(ip), i32c(n), (Value*)0);

               loadState(epPtr, heapBasePtr, envPtr, ep);
               loadState(espPtr, heapBasePtr, newEnvPtr, esp);

               // STACK[newE] <- E
               Value* stackPtr = CreateGEP(&builder, newEnvPtr, i32c(0));
               builder.CreateStore(ep, stackPtr);

               // STACK[E + 1] <- N
               Value* stackPtrInc = CreateGEP(&builder, newEnvPtr, i32c(1));
               builder.CreateStore(i32c(n), stackPtrInc);

               // E <- newE
               // newE <- E + n + 2
               builder.CreateStore(esp, epPtr);
               Value* newEsp = builder.CreateAdd(esp, i32c(n + 2));
               builder.CreateStore(newEsp, espPtr);

               // P <- P + instruction_size(P)
               ip += 2;

               break;
          }

          // deallocate:
          case DEALLOCATE:
          {
               //trace0("DEALLOCATE", ip);
               CreateTraceFn(&builder, mod, vmState->trace0, "DEALLOCATE", i32c(ip), (Value*)0);

               loadState(epPtr, heapBasePtr, envPtr, ep);

               // E <- STACK[E]
               builder.CreateStore(ep, espPtr);
               Value* oldEp = builder.CreateLoad(envPtr);
               builder.CreateStore(oldEp, epPtr);

               // P <- STACK.pop (i.e. return succesfully).
               builder.CreateRet(i32c(1));

               // P <- P + instruction_size(P)
               ip += 1;

               break;
          }

          // An unknown instruction was encountered. Something has gone wrong, so fail the compilation process.
          default:
          {
               //trace0("UNKNOWN (Fail)", ip);
               CreateTraceFn(&builder, mod, vmState->trace0, "UNKNOWN (Fail)", i32c(ip), (Value*)0);

               // fail at runtime.
               builder.CreateRet(i32c(0));

               // stop compilation.
               stopCompilation = JNI_TRUE;
          }
          }
     }

     // Pass the new function through the optimizer pipeline.
     vmState->FPM->run(*mod);

     //std::cout << "Created function: " << fName << ":\n";
     free(fName);
     //std::cout << *newFunction;

     verifyBitCode();
     writeBitCodeToFile();
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
JNIEXPORT jboolean JNICALL Java_com_thesett_aima_logic_fol_l2_L2ResolvingNativeMachine_execute
(JNIEnv * env, jobject object, jobject codeBuf, jint offset)
{
     /*std::cout << "\nJNIEXPORT jboolean JNICALL Java_com_thesett_aima_logic_fol_l2_L2ResolvingNativeMachine_execute:"
       << " called\n";*/
     //mod->print(std::cout);
     std::cerr << "\nL2 Execute\n\n";

     // Obtain the address of the compiled code to call.
     char* fName = (char*)malloc(100 * sizeof(char));
     sprintf(fName, "f_%i", offset);
     ///std::cout << "Looking up function: " << fName << "\n";

     Function *compiledQuery = vmState->EE->FindFunctionNamed(fName);

     // Call the compiled query.
     std::vector<GenericValue> noargs;
     GenericValue gv = vmState->EE->runFunction(compiledQuery, noargs);

     // Interpret the result of the execution.
     return gv.IntVal.getBoolValue();
     //return JNI_FALSE;
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
JNIEXPORT jint JNICALL Java_com_thesett_aima_logic_fol_l2_L2ResolvingNativeMachine_deref
(JNIEnv * env, jobject obj, jint a)
{
     jint addr = l2jitderef(l2state, a);
     jint heapCell = l2state->heapBasePtr[addr];
     derefTag = (jbyte)((heapCell & 0xFF000000) >> 24);
     derefVal = heapCell & 0x00FFFFFF;

     return addr;
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
JNIEXPORT jint JNICALL Java_com_thesett_aima_logic_fol_l2_L2ResolvingNativeMachine_derefStack
(JNIEnv * env, jobject obj, jint a)
{
     //fprintf(stderr, "derefStack: ep = %i\n", l2state->ep);

     jint addr = l2jitderef(l2state, a + l2state->ep + 2);
     jint heapCell = l2state->heapBasePtr[addr];
     derefTag = (jbyte)((heapCell & 0xFF000000) >> 24);
     derefVal = heapCell & 0x00FFFFFF;

     return addr;
}

/*
 * Gets the heap cell tag for the most recent dereference operation.
 *
 * @return The heap cell tag for the most recent dereference operation.
 */
JNIEXPORT jbyte JNICALL Java_com_thesett_aima_logic_fol_l2_L2ResolvingNativeMachine_getDerefTag
(JNIEnv * env, jobject obj)
{
     return derefTag;
}

/*
 * Gets the heap cell value for the most recent dereference operation.
 *
 * @return The heap cell value for the most recent dereference operation.
 */
JNIEXPORT jint JNICALL Java_com_thesett_aima_logic_fol_l2_L2ResolvingNativeMachine_getDerefVal
(JNIEnv * env, jobject obj)
{
     return derefVal;
}

/*
 * Gets the value of the heap cell at the specified location.
 *
 * @param addr The address to fetch from the heap.
 * @return The heap cell at the specified location.
 */
JNIEXPORT jint JNICALL Java_com_thesett_aima_logic_fol_l2_L2ResolvingNativeMachine_getHeap
(JNIEnv * env, jobject obj, jint addr)
{
     return l2state->heapBasePtr[addr];
}
