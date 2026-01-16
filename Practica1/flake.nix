{
  description = "PDA Practica 1 - Sistema de archivos distribuido con Python";

  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";

  outputs = { self, nixpkgs }:
    let
      pkgs = import nixpkgs { system = "x86_64-linux"; };
    in
    {
      packages.default = pkgs.mkShell {
        buildInputs = with pkgs; [
          conda-shell
        ];
      };
    };
}
