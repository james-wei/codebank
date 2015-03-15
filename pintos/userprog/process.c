#include "userprog/process.h"
#include <debug.h>
#include <inttypes.h>
#include <round.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "userprog/gdt.h"
#include "userprog/pagedir.h"
#include "userprog/tss.h"
#include "filesys/directory.h"
#include "filesys/file.h"
#include "filesys/filesys.h"
#include "threads/flags.h"
#include "threads/init.h"
#include "threads/interrupt.h"
#include "threads/malloc.h"
#include "threads/palloc.h"
#include "threads/synch.h"
#include "threads/thread.h"
#include "threads/vaddr.h"

static thread_func start_process NO_RETURN;
static bool load (const char *cmdline, void (**eip) (void), void **esp);

/* Starts a new thread running a user program loaded from
   FILENAME.  The new thread may be scheduled (and may even exit)
   before process_execute() returns.  Returns the new process's
   thread id, or TID_ERROR if the thread cannot be created. */
tid_t
process_execute (const char *file_name) 
{
  char *fn_copy,
       *prog_name_pg,
       *program_name,
       *tok_save;
  struct load_status ls;
  struct semaphore *ws_create_sema;
  struct thread *cur;
  struct wait_status *ws;
  tid_t tid;

  /* Make a copy of FILE_NAME.
     Otherwise there's a race between the caller and load(). */
  fn_copy = palloc_get_page (0);
  if (fn_copy == NULL)
    return TID_ERROR;
  strlcpy (fn_copy, file_name, PGSIZE);

  /* Make a copy of FILE_NAME.
     This is to avoid kernel permission errors when modifying
     FILE_NAME in strtok_r(). */
  prog_name_pg = palloc_get_page (0);
  if (prog_name_pg == NULL) {
    palloc_free_page (fn_copy);
    return TID_ERROR;
  }
  strlcpy (prog_name_pg, file_name, PGSIZE);

  /* Create the load_status struct. */
  ls.file_name = fn_copy;
  sema_init (&ls.load_sema, 0);
  ws_create_sema = (struct semaphore *) malloc (sizeof (struct semaphore));
  if (ws_create_sema == NULL) {
    palloc_free_page (fn_copy);
    palloc_free_page (prog_name_pg);
    return TID_ERROR;
  }
  sema_init (ws_create_sema, 0);
  ls.wait_status_create = ws_create_sema;
  ls.success = false;

  /* Decouple program name from arguments. */
  program_name = strtok_r (prog_name_pg, " ", &tok_save);

  /* Create a new thread to execute FILE_NAME. */
  tid = thread_create (program_name, PRI_DEFAULT, start_process, &ls);

  if (tid == TID_ERROR) {
    free (ws_create_sema);
    palloc_free_page (fn_copy);
    palloc_free_page (prog_name_pg);
    return TID_ERROR;
  }

  sema_down (&ls.load_sema);

  if (!ls.success) {
    /* Child thread must free ws_create_sema and fn_copy. */
    palloc_free_page (prog_name_pg);
    return TID_ERROR;
  }

  ws = (struct wait_status*) malloc (sizeof (struct wait_status));

  if (ws == NULL) {
    sema_up (ls.wait_status_create);
    /* Child thread must free ws_create_sema and fn_copy. */
    palloc_free_page (prog_name_pg);
    return TID_ERROR;
  }

  /* Initialize wait status struct. */
  cur = thread_current ();
  list_push_back (&cur->child_processes, &ws->child_elem);
  lock_init (&ws->count_lock);
  ws->count = 2;
  ws->child_tid = tid;
  ws->child_exit_code = -2;
  sema_init (&ws->child_exit_sem, 0);

  ls.child->parent_wait_status = ws;
  sema_up (ls.wait_status_create);

  /* Child thread must free ws_create_sema and fn_copy. */
  palloc_free_page (prog_name_pg);

  return tid;
}

/* A thread function that loads a user process and starts it
   running. */
static void
start_process (void *aux)
{
  bool success;
  char *curr_tok,
       *file_name,
       *program_name,
       *tok_save;
  int index;
  size_t argc = 0,
         curr_tok_len,
         fn_len;
  struct intr_frame if_;
  struct load_status *ls;
  struct semaphore *wait_status_create;

  ls = (struct load_status*) aux;
  file_name = ls->file_name;
  fn_len = strlen (ls->file_name);

  char *argv[(fn_len/2) + 1],
       **stack_argv;

  /* Decouple program name from arguments. */
  program_name = strtok_r (file_name, " ", &tok_save);
  argc++;

  /* Initialize interrupt frame and load executable. */
  memset (&if_, 0, sizeof if_);
  if_.gs = if_.fs = if_.es = if_.ds = if_.ss = SEL_UDSEG;
  if_.cs = SEL_UCSEG;
  if_.eflags = FLAG_IF | FLAG_MBS;

  success = load ((const char *) program_name, &if_.eip, &if_.esp);

  ls->success = success;
  ls->child = thread_current ();
  wait_status_create = ls->wait_status_create;
  sema_up (&ls->load_sema);

  /* If load failed, quit. */
  if (!success) {
    free (wait_status_create);
    palloc_free_page (file_name);
    thread_exit ();
  }

  /* Copy argument data values on to stack. */
  curr_tok = strtok_r (NULL, " ", &tok_save);
  while (curr_tok) {
    curr_tok_len = strlen (curr_tok) + 1;
    if_.esp = (void *) (((unsigned char *) if_.esp) - curr_tok_len);
    memcpy (if_.esp, (const void *) curr_tok, curr_tok_len);
    argv[argc] = if_.esp;
    argc++;
    curr_tok = strtok_r (NULL, " ", &tok_save);
  }

  /* Copy program name on to stack. */
  curr_tok_len = strlen (program_name) + 1;
  if_.esp = (void *) (((unsigned char *) if_.esp) - curr_tok_len);
  memcpy (if_.esp, (const void *) program_name, curr_tok_len);
  argv[0] = if_.esp;

  /* Account for word alignment on the stack. */
  while (((size_t) if_.esp) % sizeof (uint32_t) != 0) {
    if_.esp = (void *) (((unsigned char *) if_.esp) - 1);
    *((unsigned char *) if_.esp) = '\0';
  }

  /* Copy argument pointer values on to the stack. */
  index = (int) argc;
  while (index >= 0) {
    if_.esp = (void *) (((unsigned char *) if_.esp) - 4);
    if (index == (int) argc) {
      *((char **) if_.esp) = (char *) 0;
    }
    else {
      *((char **) if_.esp) = argv[index];
    }
    index--;
  }

  /* Write the value of argv on to the stack. */
  stack_argv = (char **) if_.esp;
  if_.esp = (void *) (((unsigned char *) if_.esp) - 4);
  *((char ***) if_.esp) = stack_argv;

  /* Write the value of argc on to the stack. */
  if_.esp = (void *) (((unsigned char *) if_.esp) - 4);
  *((int *) if_.esp) = (int) argc;

  /* Write the value of the zeroed return value on to the stack. */
  if_.esp = (void *) (((unsigned char *) if_.esp) - 4);
  *((void **) if_.esp) = (void *) 0;

  sema_down (wait_status_create);
  free (wait_status_create);
  palloc_free_page (file_name);

  /* Start the user process by simulating a return from an
     interrupt, implemented by intr_exit (in
     threads/intr-stubs.S).  Because intr_exit takes all of its
     arguments on the stack in the form of a `struct intr_frame',
     we just point the stack pointer (%esp) to our stack frame
     and jump to it. */
  asm volatile ("movl %0, %%esp; jmp intr_exit" : : "g" (&if_) : "memory");
  NOT_REACHED ();
}

/* Waits for thread TID to die and returns its exit status.  If
   it was terminated by the kernel (i.e. killed due to an
   exception), returns -1.  If TID is invalid or if it was not a
   child of the calling process, or if process_wait() has already
   been successfully called for the given TID, returns -1
   immediately, without waiting. */
int
process_wait (tid_t child_tid) 
{
  int exit_code;
  struct list *children;
  struct list_elem *c;
  struct thread *cur;
  struct wait_status *wstatus;

  cur = thread_current ();
  children = &cur->child_processes;

  for (c = list_begin (children); c != list_end (children);
       c = list_next (c)) {
    wstatus = list_entry (c, struct wait_status, child_elem);
    if (wstatus->child_tid == child_tid) {
      if (wstatus->child_exit_code == -1) {
        return -1;
      }
      sema_down (&wstatus->child_exit_sem);
      exit_code = wstatus->child_exit_code;
      wstatus->child_exit_code = -1;
      return exit_code;
    }
  }
  return -1;
}

/* Free the current process's resources. */
void
process_exit (void)
{
  struct thread *cur = thread_current ();
  uint32_t *pd;

  /* Allow writes and close executable file pointer. */
  if (cur->executable) {
    file_close (cur->executable);
    cur->executable = NULL;
  }

  /* Destroy the current process's page directory and switch back
     to the kernel-only page directory. */
  pd = cur->pagedir;
  if (pd != NULL) 
    {
      /* Correct ordering here is crucial.  We must set
         cur->pagedir to NULL before switching page directories,
         so that a timer interrupt can't switch back to the
         process page directory.  We must activate the base page
         directory before destroying the process's page
         directory, or our active page directory will be one
         that's been freed (and cleared). */
      cur->pagedir = NULL;
      pagedir_activate (NULL);
      pagedir_destroy (pd);
    }
  
  /* Deallocate file descriptors. */
  int fd_index;
  struct descriptor *curr_descriptor;
  struct list *free_fd;

  for (fd_index = 0; fd_index < MAX_FD; fd_index++) {
    curr_descriptor = cur->descriptors[fd_index];
    if (curr_descriptor != NULL) {
      if (curr_descriptor->file) {
        file_close (curr_descriptor->file);
      }
      free (curr_descriptor);
    }
  }

  free_fd = &cur->free_fd;
  if (!list_empty (free_fd)) {
    int j,
        num_fd_delete = 0;
    struct list_elem *fd;
    struct descriptor *descrip;
    struct descriptor* delete_descriptor[list_size (free_fd)];

    for (fd = list_begin (free_fd); fd != list_end (free_fd);
         fd = list_next (fd)) {
      descrip = list_entry (fd, struct descriptor, free_elem);
      delete_descriptor[num_fd_delete] = descrip;
      num_fd_delete++;
    }

    for (j = 0; j < num_fd_delete; j++) {
      list_remove (&delete_descriptor[j]->free_elem);
      free (delete_descriptor[j]);
    }
  }

  bool deallocate_parent;
  struct list *children;
  
  /* Deallocate dead children. */
  children = &cur->child_processes;
  if (!list_empty (children)) {
    int i, 
        num_delete = 0;
    struct list_elem *c;
    struct wait_status *wstatus;
    struct wait_status* delete_children[list_size (children)];

    for (c = list_begin (children); c != list_end (children);
         c = list_next (c)) {
      wstatus = list_entry (c, struct wait_status, child_elem);
      lock_acquire (&wstatus->count_lock);
      wstatus->count--;
      if (wstatus->count == 0) {
        delete_children[num_delete] = wstatus;
        num_delete++;
      }
      lock_release (&wstatus->count_lock);
    }

    for (i = 0; i < num_delete; i++) {
      list_remove (&delete_children[i]->child_elem);
      free (delete_children[i]);
    }
  }

  /* Deallocate dead parent. */
  deallocate_parent = false;
  if (cur->parent_wait_status != NULL) {
    lock_acquire (&cur->parent_wait_status->count_lock);
    cur->parent_wait_status->count--;
    if (cur->parent_wait_status->count == 0) {
      deallocate_parent = true;
    }
    lock_release (&cur->parent_wait_status->count_lock);  

    if (deallocate_parent) {
      free(cur->parent_wait_status);
    }
    else {
      sema_up (&cur->parent_wait_status->child_exit_sem);
    }
  }
}

/* Sets up the CPU for running user code in the current
   thread.
   This function is called on every context switch. */
void
process_activate (void)
{
  struct thread *t = thread_current ();

  /* Activate thread's page tables. */
  pagedir_activate (t->pagedir);

  /* Set thread's kernel stack for use in processing
     interrupts. */
  tss_update ();
}

/* We load ELF binaries.  The following definitions are taken
   from the ELF specification, [ELF1], more-or-less verbatim.  */

/* ELF types.  See [ELF1] 1-2. */
typedef uint32_t Elf32_Word, Elf32_Addr, Elf32_Off;
typedef uint16_t Elf32_Half;

/* For use with ELF types in printf(). */
#define PE32Wx PRIx32   /* Print Elf32_Word in hexadecimal. */
#define PE32Ax PRIx32   /* Print Elf32_Addr in hexadecimal. */
#define PE32Ox PRIx32   /* Print Elf32_Off in hexadecimal. */
#define PE32Hx PRIx16   /* Print Elf32_Half in hexadecimal. */

/* Executable header.  See [ELF1] 1-4 to 1-8.
   This appears at the very beginning of an ELF binary. */
struct Elf32_Ehdr
  {
    unsigned char e_ident[16];
    Elf32_Half    e_type;
    Elf32_Half    e_machine;
    Elf32_Word    e_version;
    Elf32_Addr    e_entry;
    Elf32_Off     e_phoff;
    Elf32_Off     e_shoff;
    Elf32_Word    e_flags;
    Elf32_Half    e_ehsize;
    Elf32_Half    e_phentsize;
    Elf32_Half    e_phnum;
    Elf32_Half    e_shentsize;
    Elf32_Half    e_shnum;
    Elf32_Half    e_shstrndx;
  };

/* Program header.  See [ELF1] 2-2 to 2-4.
   There are e_phnum of these, starting at file offset e_phoff
   (see [ELF1] 1-6). */
struct Elf32_Phdr
  {
    Elf32_Word p_type;
    Elf32_Off  p_offset;
    Elf32_Addr p_vaddr;
    Elf32_Addr p_paddr;
    Elf32_Word p_filesz;
    Elf32_Word p_memsz;
    Elf32_Word p_flags;
    Elf32_Word p_align;
  };

/* Values for p_type.  See [ELF1] 2-3. */
#define PT_NULL    0            /* Ignore. */
#define PT_LOAD    1            /* Loadable segment. */
#define PT_DYNAMIC 2            /* Dynamic linking info. */
#define PT_INTERP  3            /* Name of dynamic loader. */
#define PT_NOTE    4            /* Auxiliary info. */
#define PT_SHLIB   5            /* Reserved. */
#define PT_PHDR    6            /* Program header table. */
#define PT_STACK   0x6474e551   /* Stack segment. */

/* Flags for p_flags.  See [ELF3] 2-3 and 2-4. */
#define PF_X 1          /* Executable. */
#define PF_W 2          /* Writable. */
#define PF_R 4          /* Readable. */

static bool setup_stack (void **esp);
static bool validate_segment (const struct Elf32_Phdr *, struct file *);
static bool load_segment (struct file *file, off_t ofs, uint8_t *upage,
                          uint32_t read_bytes, uint32_t zero_bytes,
                          bool writable);

/* Loads an ELF executable from FILE_NAME into the current thread.
   Stores the executable's entry point into *EIP
   and its initial stack pointer into *ESP.
   Returns true if successful, false otherwise. */
bool
load (const char *file_name, void (**eip) (void), void **esp) 
{
  struct thread *t = thread_current ();
  struct Elf32_Ehdr ehdr;
  struct file *file = NULL;
  off_t file_ofs;
  bool success = false;
  int i;

  /* Allocate and activate page directory. */
  t->pagedir = pagedir_create ();
  if (t->pagedir == NULL) 
    goto done;
  process_activate ();

  /* Open executable file. */
  file = filesys_open (file_name);
  if (file == NULL) 
    {
      printf ("load: %s: open failed\n", file_name);
      goto done; 
    }

  t->executable = file;
  file_deny_write (file);

  /* Read and verify executable header. */
  if (file_read (file, &ehdr, sizeof ehdr) != sizeof ehdr
      || memcmp (ehdr.e_ident, "\177ELF\1\1\1", 7)
      || ehdr.e_type != 2
      || ehdr.e_machine != 3
      || ehdr.e_version != 1
      || ehdr.e_phentsize != sizeof (struct Elf32_Phdr)
      || ehdr.e_phnum > 1024) 
    {
      printf ("load: %s: error loading executable\n", file_name);
      goto done; 
    }

  /* Read program headers. */
  file_ofs = ehdr.e_phoff;
  for (i = 0; i < ehdr.e_phnum; i++) 
    {
      struct Elf32_Phdr phdr;

      if (file_ofs < 0 || file_ofs > file_length (file))
        goto done;
      file_seek (file, file_ofs);

      if (file_read (file, &phdr, sizeof phdr) != sizeof phdr)
        goto done;
      file_ofs += sizeof phdr;
      switch (phdr.p_type) 
        {
        case PT_NULL:
        case PT_NOTE:
        case PT_PHDR:
        case PT_STACK:
        default:
          /* Ignore this segment. */
          break;
        case PT_DYNAMIC:
        case PT_INTERP:
        case PT_SHLIB:
          goto done;
        case PT_LOAD:
          if (validate_segment (&phdr, file)) 
            {
              bool writable = (phdr.p_flags & PF_W) != 0;
              uint32_t file_page = phdr.p_offset & ~PGMASK;
              uint32_t mem_page = phdr.p_vaddr & ~PGMASK;
              uint32_t page_offset = phdr.p_vaddr & PGMASK;
              uint32_t read_bytes, zero_bytes;
              if (phdr.p_filesz > 0)
                {
                  /* Normal segment.
                     Read initial part from disk and zero the rest. */
                  read_bytes = page_offset + phdr.p_filesz;
                  zero_bytes = (ROUND_UP (page_offset + phdr.p_memsz, PGSIZE)
                                - read_bytes);
                }
              else 
                {
                  /* Entirely zero.
                     Don't read anything from disk. */
                  read_bytes = 0;
                  zero_bytes = ROUND_UP (page_offset + phdr.p_memsz, PGSIZE);
                }
              if (!load_segment (file, file_page, (void *) mem_page,
                                 read_bytes, zero_bytes, writable))
                goto done;
            }
          else
            goto done;
          break;
        }
    }

  /* Set up stack. */
  if (!setup_stack (esp))
    goto done;

  /* Start address. */
  *eip = (void (*) (void)) ehdr.e_entry;

  success = true;

 done:
  /* We arrive here whether the load is successful or not. */
  return success;
}

/* load() helpers. */

static bool install_page (void *upage, void *kpage, bool writable);

/* Checks whether PHDR describes a valid, loadable segment in
   FILE and returns true if so, false otherwise. */
static bool
validate_segment (const struct Elf32_Phdr *phdr, struct file *file) 
{
  /* p_offset and p_vaddr must have the same page offset. */
  if ((phdr->p_offset & PGMASK) != (phdr->p_vaddr & PGMASK)) 
    return false; 

  /* p_offset must point within FILE. */
  if (phdr->p_offset > (Elf32_Off) file_length (file)) 
    return false;

  /* p_memsz must be at least as big as p_filesz. */
  if (phdr->p_memsz < phdr->p_filesz) 
    return false; 

  /* The segment must not be empty. */
  if (phdr->p_memsz == 0)
    return false;
  
  /* The virtual memory region must both start and end within the
     user address space range. */
  if (!is_user_vaddr ((void *) phdr->p_vaddr))
    return false;
  if (!is_user_vaddr ((void *) (phdr->p_vaddr + phdr->p_memsz)))
    return false;

  /* The region cannot "wrap around" across the kernel virtual
     address space. */
  if (phdr->p_vaddr + phdr->p_memsz < phdr->p_vaddr)
    return false;

  /* Disallow mapping page 0.
     Not only is it a bad idea to map page 0, but if we allowed
     it then user code that passed a null pointer to system calls
     could quite likely panic the kernel by way of null pointer
     assertions in memcpy(), etc. */
  if (phdr->p_vaddr < PGSIZE)
    return false;

  /* It's okay. */
  return true;
}

/* Loads a segment starting at offset OFS in FILE at address
   UPAGE.  In total, READ_BYTES + ZERO_BYTES bytes of virtual
   memory are initialized, as follows:

        - READ_BYTES bytes at UPAGE must be read from FILE
          starting at offset OFS.

        - ZERO_BYTES bytes at UPAGE + READ_BYTES must be zeroed.

   The pages initialized by this function must be writable by the
   user process if WRITABLE is true, read-only otherwise.

   Return true if successful, false if a memory allocation error
   or disk read error occurs. */
static bool
load_segment (struct file *file, off_t ofs, uint8_t *upage,
              uint32_t read_bytes, uint32_t zero_bytes, bool writable) 
{
  ASSERT ((read_bytes + zero_bytes) % PGSIZE == 0);
  ASSERT (pg_ofs (upage) == 0);
  ASSERT (ofs % PGSIZE == 0);

  file_seek (file, ofs);
  while (read_bytes > 0 || zero_bytes > 0) 
    {
      /* Calculate how to fill this page.
         We will read PAGE_READ_BYTES bytes from FILE
         and zero the final PAGE_ZERO_BYTES bytes. */
      size_t page_read_bytes = read_bytes < PGSIZE ? read_bytes : PGSIZE;
      size_t page_zero_bytes = PGSIZE - page_read_bytes;

      /* Get a page of memory. */
      uint8_t *kpage = palloc_get_page (PAL_USER);
      if (kpage == NULL)
        return false;

      /* Load this page. */
      if (file_read (file, kpage, page_read_bytes) != (int) page_read_bytes)
        {
          palloc_free_page (kpage);
          return false; 
        }
      memset (kpage + page_read_bytes, 0, page_zero_bytes);

      /* Add the page to the process's address space. */
      if (!install_page (upage, kpage, writable)) 
        {
          palloc_free_page (kpage);
          return false; 
        }

      /* Advance. */
      read_bytes -= page_read_bytes;
      zero_bytes -= page_zero_bytes;
      upage += PGSIZE;
    }
  return true;
}

/* Create a minimal stack by mapping a zeroed page at the top of
   user virtual memory. */
static bool
setup_stack (void **esp) 
{
  uint8_t *kpage;
  bool success = false;

  kpage = palloc_get_page (PAL_USER | PAL_ZERO);
  if (kpage != NULL) 
    {
      success = install_page (((uint8_t *) PHYS_BASE) - PGSIZE, kpage, true);
      if (success)
        *esp = PHYS_BASE;
      else
        palloc_free_page (kpage);
    }
  return success;
}

/* Adds a mapping from user virtual address UPAGE to kernel
   virtual address KPAGE to the page table.
   If WRITABLE is true, the user process may modify the page;
   otherwise, it is read-only.
   UPAGE must not already be mapped.
   KPAGE should probably be a page obtained from the user pool
   with palloc_get_page().
   Returns true on success, false if UPAGE is already mapped or
   if memory allocation fails. */
static bool
install_page (void *upage, void *kpage, bool writable)
{
  struct thread *t = thread_current ();

  /* Verify that there's not already a page at that virtual
     address, then map our page there. */
  return (pagedir_get_page (t->pagedir, upage) == NULL
          && pagedir_set_page (t->pagedir, upage, kpage, writable));
}
