#include "userprog/syscall.h"
#include <stdio.h>
#include <syscall-nr.h>
#include "devices/input.h"
#include "devices/shutdown.h"
#include "filesys/filesys.h"
#include "filesys/file.h"
#include "threads/interrupt.h"
#include "threads/synch.h"
#include "threads/thread.h"
#include "threads/vaddr.h"
#include "threads/malloc.h"
#include "userprog/pagedir.h"
#include "userprog/process.h"

static void syscall_handler (struct intr_frame *);
static bool valid_user_addr(uint32_t *);
static void force_exit(struct intr_frame *);

static void syscall_halt (void);
static void syscall_exit (struct intr_frame *, uint32_t *);
static void syscall_exec (struct intr_frame *, uint32_t *);
static void syscall_wait (struct intr_frame *, uint32_t *);
static void syscall_create (struct intr_frame *, uint32_t *);
static void syscall_remove (struct intr_frame *, uint32_t *);
static void syscall_open (struct intr_frame *, uint32_t *);
static void syscall_filesize (struct intr_frame *, uint32_t *);
static void syscall_read (struct intr_frame *, uint32_t *);
static void syscall_write (struct intr_frame *, uint32_t *);
static void syscall_seek (struct intr_frame *, uint32_t *);
static void syscall_tell (struct intr_frame *, uint32_t *);
static void syscall_close (struct intr_frame *, uint32_t *);
static void syscall_null (struct intr_frame *, uint32_t *);

static struct lock filesys_lock;

void
syscall_init (void) 
{
  intr_register_int (0x30, 3, INTR_ON, syscall_handler, "syscall");
  lock_init (&filesys_lock);
}

static void
syscall_handler (struct intr_frame *f) 
{
  uint32_t* args = ((uint32_t*) f->esp);

  if (!valid_user_addr (args)) {
    return force_exit (f);
  }

  switch (args[0]) {
    case SYS_HALT:
      syscall_halt ();
      break;
    case SYS_EXIT:
      syscall_exit (f, args);
      break;
    case SYS_EXEC:
      syscall_exec (f, args);
      break;
    case SYS_WAIT:
      syscall_wait (f, args);
      break;
    case SYS_CREATE:
      syscall_create (f, args);
      break;
    case SYS_REMOVE:
      syscall_remove (f, args);
      break;
    case SYS_OPEN:
      syscall_open (f, args);
      break;
    case SYS_FILESIZE:
      syscall_filesize (f, args);
      break;
    case SYS_READ:
      syscall_read (f, args);
      break;
    case SYS_WRITE:
      syscall_write (f, args);
      break;
    case SYS_SEEK:
      syscall_seek (f, args);
      break;
    case SYS_TELL:
      syscall_tell (f, args);
      break;
    case SYS_CLOSE:
      syscall_close (f, args);
      break;
    case SYS_NULL:
      syscall_null (f, args);
      break;
    default:
      force_exit (f);
      break;
  }
}

static bool
valid_user_addr(uint32_t *uaddr)
{
  struct thread *cur = thread_current ();
  void *page;

  if (uaddr == NULL || (void *) (uaddr + 1) > PHYS_BASE) {
    return false;
  }
  page = pagedir_get_page (cur->pagedir, uaddr);
  return (page != NULL);
}

static void
force_exit(struct intr_frame *f)
{
  struct thread *cur = thread_current ();
  if (cur->parent_wait_status != NULL) {
    cur->parent_wait_status->child_exit_code = -1;
  }
  f->eax = -1;
  printf ("%s: exit(%d)\n", cur->name, -1);
  thread_exit ();
}

static void
syscall_halt (void)
{
  shutdown_power_off ();
}

static void
syscall_exit (struct intr_frame *f, uint32_t* args)
{
  if (!valid_user_addr (args + 1)) {
    force_exit (f);
  }
  else {
    struct thread *cur = thread_current ();
    if (cur->parent_wait_status != NULL) {
      cur->parent_wait_status->child_exit_code = (int) args[1];
    }
    f->eax = args[1];
    printf ("%s: exit(%d)\n", cur->name, (int) args[1]);
    thread_exit ();
  }
}

static void
syscall_exec (struct intr_frame *f, uint32_t* args)
{
  if (!valid_user_addr (args + 1) ||
      !valid_user_addr ((uint32_t *) args[1])) {
    force_exit (f);
  }
  else {
    f->eax = process_execute ((const char *) args[1]);
  }
}

static void
syscall_wait (struct intr_frame *f, uint32_t* args)
{
  if (!valid_user_addr (args + 1)) {
    force_exit (f);
  }
  else {
    f->eax = process_wait ((tid_t) args[1]);
  }
}

static void
syscall_create (struct intr_frame *f, uint32_t* args)
{
  if (!valid_user_addr (args + 1) ||
      !valid_user_addr ((uint32_t *) args[1]) ||
      !valid_user_addr (args + 2)) {
    force_exit (f);
  }
  else {
    lock_acquire (&filesys_lock);
    f->eax = filesys_create ((const char*) args[1], (off_t) args[2]);
    lock_release (&filesys_lock);
  }
}

static void
syscall_remove (struct intr_frame *f, uint32_t* args)
{
  if (!valid_user_addr (args + 1) ||
      !valid_user_addr ((uint32_t *) args[1])) {
    force_exit (f);
  }
  else {
    lock_acquire (&filesys_lock);
    f->eax = filesys_remove((const char*) args[1]);
    lock_release (&filesys_lock);
  }
}

static void
syscall_open (struct intr_frame *f, uint32_t* args)
{
  if (!valid_user_addr (args + 1) ||
      !valid_user_addr ((uint32_t *) args[1])) {
    force_exit (f);
  }
  else {
    struct descriptor *d;
    struct file *file;
    struct list_elem *e;
    struct thread *cur;

    cur = thread_current ();

    /* Check if there is a free file descriptor. */
    if (!list_empty (&cur->free_fd)) {
      lock_acquire (&filesys_lock);
      file = filesys_open ((const char*) args[1]);
      lock_release (&filesys_lock);
      if (file == NULL) {
        f->eax = -1;
      }
      else {
        e = list_begin (&cur->free_fd);
        d = list_entry (e, struct descriptor, free_elem);
        d->file = file;
        list_remove (e);
        cur->descriptors[d->fd - 2] = d;
        f->eax = d->fd;
      }
    }
    /* Add a new file descriptor if possible. */
    else if (cur->last_fd_created < MAX_FD - 1) {
      lock_acquire (&filesys_lock);
      file = filesys_open ((const char*) args[1]);
      lock_release (&filesys_lock);
      if (file == NULL) {
        f->eax = -1;
      }
      else {
        d = (struct descriptor*) malloc (sizeof (struct descriptor));
        if (d == NULL) {
          file_close (file);
          f->eax = -1;
        }
        else {
          d->file = file;
          cur->last_fd_created++;
          d->fd = cur->last_fd_created + 2;
          cur->descriptors[cur->last_fd_created] = d;
          f->eax = d->fd;
        }
      }
    }
    /* Max file descriptors reached. */
    else {
      f->eax = -1;
    }
  }
}

static void
syscall_filesize (struct intr_frame *f, uint32_t* args)
{
  if (!valid_user_addr (args + 1)) {
    force_exit (f);
  }
  else {
    int fd = args[1];
    struct descriptor *d;
    if (fd < 2 || fd - 2 >= MAX_FD) {
      f->eax = -1;
    }
    else {
      d = thread_current ()->descriptors[fd - 2];
      if (d == NULL || d->file == NULL) {
        f->eax = -1;
      }
      else {
        lock_acquire (&filesys_lock);
        f->eax = file_length (d->file);
        lock_release (&filesys_lock);
      }
    }
  }
}

static void
syscall_read (struct intr_frame *f, uint32_t* args)
{
  if (!valid_user_addr (args + 1) ||
      !valid_user_addr (args + 2) ||
      !valid_user_addr ((uint32_t *) args[2]) ||
      !valid_user_addr (args + 3)) {
    force_exit (f);
  }
  else {
    int fd = args[1];
    struct descriptor *d;
    uint8_t *buffer = (uint8_t *) args[2];
    unsigned i,
             size = args[3];

    if (fd == 0) {
      for (i = 0; i < size; i++) {
        buffer[i] = input_getc ();
      }
    }
    else if (fd - 2 < 0 || fd - 2 >= MAX_FD) {
      f->eax = -1;
    }
    else {
      d = thread_current ()->descriptors[fd - 2];
      if (d == NULL || d->file == NULL) {
        f->eax = -1;
      }
      else {
        lock_acquire (&filesys_lock);
        f->eax = file_read (d->file, (void *) buffer, size);
        lock_release (&filesys_lock);
      }
    }
  }
}

static void
syscall_write (struct intr_frame *f, uint32_t* args)
{
  if (!valid_user_addr (args + 1) ||
      !valid_user_addr (args + 2) ||
      !valid_user_addr ((uint32_t *) args[2])  ||
      !valid_user_addr (args + 3)) {
    force_exit (f);
  }
  else {
    int fd = args[1];
    const char *buffer = (const char *) args[2];
    struct descriptor *d;
    unsigned size = args[3];

    if (fd == 1) {
      /* Write to console. */
      lock_acquire (&filesys_lock);
      putbuf (buffer, (size_t) size);
      lock_release (&filesys_lock);
      f->eax = size;
    }
    else if (fd - 2 < 0 || fd - 2 >= MAX_FD) {
      f->eax = -1;
    }
    else {
      d = thread_current ()->descriptors[fd - 2];
      if (d == NULL || d->file == NULL) {
        f->eax = -1;
      }
      else {
        lock_acquire (&filesys_lock);
        f->eax = file_write (d->file, (void *) buffer, size);
        lock_release (&filesys_lock);
      }
    }
  }
}

static void
syscall_seek (struct intr_frame *f, uint32_t* args)
{
  if (!valid_user_addr (args + 1) ||
      !valid_user_addr (args + 2)) {
    force_exit (f);
  }
  else {
    int fd = args[1];
    if (fd < 2 || fd - 2 >= MAX_FD) {
      f->eax = -1;
    }
    else {
      struct descriptor *d = thread_current ()->descriptors[fd - 2];
      if (d == NULL || d->file == NULL) {
        f->eax = -1;
      }
      else {
        lock_acquire (&filesys_lock);
        file_seek (d->file, (off_t) args[2]);
        lock_release (&filesys_lock);
      }
    }
  }
}

static void
syscall_tell (struct intr_frame *f, uint32_t* args)
{
  if (!valid_user_addr (args + 1)) {
    force_exit (f);
  }
  else {
    int fd = args[1];
    if (fd < 2 || fd - 2 >= MAX_FD) {
      f->eax = -1;
    }
    else {
      struct descriptor *d = thread_current ()->descriptors[fd - 2];
      if (d == NULL || d->file == NULL) {
        f->eax = -1;
      }
      else {
        lock_acquire (&filesys_lock);
        f->eax = file_tell (d->file);
        lock_release (&filesys_lock);
      }
    }
  }
}

static void
syscall_close (struct intr_frame *f, uint32_t* args)
{
  if (!valid_user_addr (args + 1)) {
    force_exit (f);
  }
  else {
    int fd = args[1];
    struct thread *cur = thread_current ();
    if (fd < 2 || fd - 2 >= MAX_FD) {
      f->eax = -1;
    }
    else {
      struct descriptor *d = cur->descriptors[fd - 2];
      if (d == NULL || d->file == NULL) {
        f->eax = -1;
      }
      else {
        lock_acquire (&filesys_lock);
        file_close (d->file);
        lock_release (&filesys_lock);
        d->file = NULL;
        cur->descriptors[fd - 2] = NULL;
        list_push_back (&cur->free_fd, &d->free_elem);
      }
    }
  }
}

static void
syscall_null (struct intr_frame *f, uint32_t* args)
{
  if (!valid_user_addr (args + 1)) {
    force_exit (f);
  }
  else {
    f->eax = args[1] + 1;
  }
}
