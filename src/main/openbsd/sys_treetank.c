/*
 * Copyright (c) 2008, Marc Kramis (Ph.D. Thesis), University of Konstanz
 * 
 * Patent Pending.
 * 
 * NO permission to use, copy, modify, and/or distribute this software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 * 
 * $Id$
 */

#include "sys_treetank.h"

/** === TreeTank SysCall =======================================================
  * Used to TT_COMPRESS, encrypt, authenticate, and write buffer to device.
  * 
  * The following steps allow to rebuild the OpenBSD kernel:
  * 1) Add following line to end of /usr/src/sys/kern/syscalls.master:
  * 306	STD		{ int sys_treetank( \
  *                u_int8_t  tank, \
  *                u_int8_t  operation, \
  *                u_int16_t length, \
  *                u_int8_t *bufferPtr); }
  *
  * 2) Rebuild syscall entries:
  * # cd /usr/src/sys/kern
  * # make init_sysent.c at /usr/src/sys/kern
  * 
  * 3) Add the following line in alphabetic order to /usr/src/sys/conf/files:
  * file sys_treetank_compression.c
  * file sys_treetank_encryption.c
  * file sys_treetank_authentication.c
  * file sys_treetank.c
  *
  * 4) BUGFIX: Add the following after line 2325 
       in /usr/src/sys/dev/pci/hifn7751.c:
  * if ((crd->crd_flags & CRD_F_IV_PRESENT)
  *    != 0)
  *    continue;
  *
  * 5) Comment out the following line from /usr/src/sys/arch/i386/conf/GENERIC:
  * glxsb* at pci?
  *
  * 6) create GENERIC configuration:
  * # cd /usr/src/sys/arch/i386/conf
  * # config GENERIC
  *
  * 7) Rebuild kernel:
  * # cd /usr/src/sys/arch/i386/compile/GENERIC
  * # make clean && make depend && make
  * # make install
  * # reboot
  * ========================================================================= */
  
/* --- Function prototypes. ------------------------------------------------- */

int sys_treetank_compression(u_int8_t, u_int8_t, u_int8_t *, u_int8_t *);
int sys_treetank_encryption(u_int8_t, u_int8_t, u_int8_t *, u_int8_t *);
int sys_treetank_authentication(u_int8_t, u_int8_t, u_int8_t *, u_int8_t *, u_int8_t *);

/* --- Global variables. ---------------------------------------------------- */

static u_int8_t tt_reference[TT_CORE_COUNT][TT_REFERENCE_LENGTH];
static u_int8_t tt_buffer[TT_CORE_COUNT][TT_BUFFER_LENGTH];

/* --- Code. ---------------------------------------------------------------- */

/*
 * Entry point for sys_treetank system call.
 */
int
sys_treetank(struct proc *p, void *v, register_t *retval)
{

  /* --- Local variables. --------------------------------------------------- */
  
  int                       result           = TT_ERROR;
  struct sys_treetank_args *argumentPointer  = v;
  
  u_int8_t                  tank             = SCARG(argumentPointer, tank);
  u_int8_t                  operation        = SCARG(argumentPointer, operation);
  u_int16_t                 length           = SCARG(argumentPointer, length);
  u_int8_t                 *bufferPtr        = SCARG(argumentPointer, bufferPtr);
  
  u_int8_t                 *startPointer     = NULL;
  u_int8_t                 *lengthPointer    = NULL;
  u_int8_t                 *hmacPointer      = NULL;
  
  /* --- Copy reference and buffer from user space. ------------------------- */
  
  copyin(
    referencePointer,
    tt_reference[core],
    TT_REFERENCE_LENGTH);
    
  startPointer  = tt_reference[core] + TT_START_OFFSET;
  lengthPointer = tt_reference[core] + TT_LENGTH_OFFSET;
  hmacPointer   = tt_reference[core] + TT_HMAC_OFFSET;
  
  if ((TT_READ_INT(lengthPointer) <= 0)
      || (TT_READ_INT(lengthPointer) > TT_BUFFER_LENGTH)) {
    error = TT_ERROR;
    printf("ERROR(sys_treetank.c): Invalid length.\n");
    goto finish;
  }
    
  copyin(
    bufferPointer,
    tt_buffer[core],
    TT_READ_INT(lengthPointer));
    
  /* --- Perform operations. ------------------------------------------------ */
    
  if (operation == TT_WRITE) {
  
      if (sys_treetank_compression(
            core,
            operation,
            lengthPointer,
            tt_buffer[core]) != TT_OK) {
        error = TT_ERROR;
        printf("ERROR(sys_treetank.c): Could not perform compression.\n");
        goto finish;
      }
      
      if (sys_treetank_encryption(
            core,
            operation,
            lengthPointer,
            tt_buffer[core]) != TT_OK) {
        error = TT_ERROR;
        printf("ERROR(sys_treetank.c): Could not perform encryption.\n");
        goto finish;
      }
      
      if (sys_treetank_authentication(
            core,
            operation,
            lengthPointer,
            hmacPointer,
            tt_buffer[core]) != TT_OK) {
        error = TT_ERROR;
        printf("ERROR(sys_treetank.c): Could not perform authentication.\n");
        goto finish;
      }
      
  } else {
  
      if (sys_treetank_authentication(
            core,
            operation,
            lengthPointer,
            hmacPointer,
            tt_buffer[core]) != TT_OK) {
        error = TT_ERROR;
        printf("ERROR(sys_treetank.c): Could not perform authentication.\n");
        goto finish;
      }
      
      if (sys_treetank_encryption(
            core,
            operation,
            lengthPointer,
            tt_buffer[core]) != TT_OK) {
        error = TT_ERROR;
        printf("ERROR(sys_treetank.c): Could not perform encryption.\n");
        goto finish;
      }
  
      if (sys_treetank_compression(
            core,
            operation,
            lengthPointer,
            tt_buffer[core]) != TT_OK) {
        error = TT_ERROR;
        printf("ERROR(sys_treetank.c): Could not perform compression.\n");
        goto finish;
      }
  
  }
  
  /* --- Copy buffer and reference to user space. --------------------------- */
  
  copyout(
    tt_buffer[core],
    bufferPointer,
    TT_READ_INT(lengthPointer));
    
  copyout(
    tt_reference[core],
    referencePointer,
    TT_REFERENCE_LENGTH);
  
  /* --- Cleanup for all conditions. ---------------------------------------- */
  
finish:
  
  return (error);
  
}