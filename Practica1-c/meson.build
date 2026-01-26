project('pda', 'c')
thread_dep = dependency('threads')
readline_dep = dependency('readline')
inc = include_directories('include')

sources = [
  'src/main.c',
  'src/protocolo.c',
  'src/almacenamiento.c',
  'src/red.c',
  'src/master.c',
  'src/list.c',
  
]

src_shell = [
  'src/shell.c'
]
executable('touche', sources, include_directories : inc, dependencies: thread_dep)
executable('shell', src_shell, dependencies: [thread_dep, readline_dep])
