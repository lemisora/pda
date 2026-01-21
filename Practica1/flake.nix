{
  description = "PDA Practica 1 - Sistema de archivos distribuido con Python";

  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";

  outputs =
    { self, nixpkgs }:
    let
      system = "x86_64-linux";
      pkgs = import nixpkgs { inherit system; };
      ENV_NAME = "pda";
    in
    {
      devShells.${system}.default = pkgs.mkShell {
        packages = with pkgs; [
          conda
        ];
        shellHook = ''
          # eval "$(conda shell.bash hook)"
          # Crear el entorno si la carpeta no existe
          ENV_DIR=$HOME/.conda/envs/${ENV_NAME};
          if [ ! -d "$ENV_DIR" ]; then
            echo "$ENV_DIR no existe, creando..."
            echo "Creando entorno ${ENV_NAME} de Conda en $ENV_DIR..."
          # Instalamos python y colorama directamente
            conda create -n ${ENV_NAME} python=3.12 colorama -y
          else
            echo "Entorno ${ENV_NAME} ya existe en $ENV_DIR"
          fi
          
          # "Activación" manual: exportamos el PATH de los binarios
          # Esto funciona para Fish, Bash y Zed porque solo inyecta variables
          export CONDA_PREFIX="$ENV_DIR"
          export PATH="$ENV_DIR/bin:$PATH"
          echo "Entorno '${ENV_NAME}' inyectado en el PATH"
        '';
      };
    };
}