project('pda', 'c')

thread_dep = dependency('threads')
readline_dep = dependency('readline')

inc = include_directories('include')

sources = [
  'main.c',
  'protocolo.c',
  'master.c',
  'worker.c',
  'dyn_list.c'
]

src_shell = [
  'shell.c',
  'protocolo.c',
]
executable('touche', 
            sources, 
            include_directories : inc, 
            dependencies: thread_dep)
executable('shell', 
            src_shell,
            include_directories : inc,
            dependencies: [thread_dep, readline_dep])
# executable('main', sources, include_directories : inc)