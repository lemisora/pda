project('pda', 'c')
thread_dep = dependency('threads')
inc = include_directories('include')
sources = [
  'src/main.c',
  'src/protocolo.c',
  'src/almacenamiento.c',
  'src/red.c'
]
executable('touche', sources, include_directories : inc, dependencies: thread_dep)