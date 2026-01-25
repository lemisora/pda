{
  description = "PDA Practica 1 - Sistema de archivos distribuidos usando C";

  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs?ref=nixos-unstable";
  };

  outputs =
    { self, nixpkgs }:
    let
      system = "x86_64-linux";
      pkgs = import nixpkgs { inherit system; };
    in
    {
      devShells.${system}.default = pkgs.mkShell {
        packages = with pkgs; [
          clang
          clang-tools
          meson
          ninja
          pkg-config
          gdb
          glibc.dev
        ];

        shellHook = ''
          # Ayuda extra para que las herramientas encuentren la libc
          export C_INCLUDE_PATH="${pkgs.glibc.dev}/include:$C_INCLUDE_PATH"
          # export CPLUS_INCLUDE_PATH="${pkgs.glibc.dev}/include:$CPLUS_INCLUDE_PATH"
          # export LIBRARY_PATH="${pkgs.glibc}/lib"
          echo "Entorno de desarrollo cargado."
        '';
      };
    };
}
