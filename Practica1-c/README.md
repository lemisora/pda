# Sistema de archivos distribuido de prueba hecho en C

## Para desarrollo

Para facilitar la integración del entorno de desarrollo se usan los flakes de Nix, para que no necesite instalar las dependencias manualmente.

### Dependencias

Para que esto funcione correctamente, necesita las siguientes dependencias:

- **Nix**
  1. Primero, instale Nix. Para una instalación sencilla de un solo usuario, ejecute:
     ```bash
     sh <(curl --proto '=https' --tlsv1.2 -L https://nixos.org/nix/install) --no-daemon
     ```
  2. Habilite los `flakes` para este proyecto. Cree el directorio `/etc/nix` (si no existe) y edite el archivo `/etc/nix/nix.conf` para añadir la siguiente línea:
     ```bash
     experimental-features = nix-command flakes
     ```

- **Direnv**
  1. Una vez instalado Nix, instale Direnv con:
     ```bash
     nix profile install nixpkgs#direnv
     ```
  2. Para que el entorno se recargue automáticamente, añada la siguiente línea a su `.bashrc`:
     ```bash
     eval "$(direnv hook bash)"
     ```

- **Devenv**
  1. Una vez instalado Nix, instale Devenv con:
     ```bash
     nix profile install nixpkgs#devenv
     ```