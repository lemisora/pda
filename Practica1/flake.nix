{
  description = "PDA Practica 1 - Sistema de archivos distribuido con Python";

  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";

  outputs = { self, nixpkgs }:
    let
      system = "x86_64-linux";
      pkgs = import nixpkgs { inherit system; };
    in
    {
      devShells.${system}.default = pkgs.mkShell {
        packages = with pkgs; [
          conda
        ];
        shellHook = ''
          echo "Entering the PDA Practica 1 environment"
        '';
      };
    };
}