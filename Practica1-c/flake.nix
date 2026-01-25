{
  description = "PDA Practica 1 - Sistema de archivos distribuidos usando C";

  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs?ref=nixos-unstable";
  };

  outputs = { self, nixpkgs }: 
  let
    system = "x86_64-linux";
    pkgs = import nixpkgs { inherit system; };
  in
  {
    devShells.${system}.default = pkgs.mkShell {
      packages = with pkgs; [
        clang
        meson
        ninja
        pkg-config
      ];
    };
  };
}
