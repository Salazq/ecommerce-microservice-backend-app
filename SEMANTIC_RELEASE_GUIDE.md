# Semantic Versioning and Release Notes Setup

Este documento explica cómo utilizar el sistema de versionado semántico y generación automática de release notes implementado en el proyecto.

## 📋 Tabla de Contenidos

- [Instalación](#instalación)
- [Configuración](#configuración)
- [Uso](#uso)
- [Conventional Commits](#conventional-commits)
- [Integración con Azure DevOps](#integración-con-azure-devops)
- [Troubleshooting](#troubleshooting)

## 🚀 Instalación

### Prerequisitos

1. **Node.js 18+**: Requerido para semantic-release
2. **npm**: Para instalar las dependencias
3. **Git**: Para el control de versiones
4. **Maven**: Para el build del proyecto Java

### Instalar dependencias

```powershell
# Instalar dependencias de Node.js
npm install
```

## ⚙️ Configuración

### Archivos de configuración creados:

1. **`.releaserc`**: Configuración principal de semantic-release
2. **`package.json`**: Dependencias y scripts de Node.js
3. **`scripts/update-version.js`**: Script para actualizar versiones en pom.xml
4. **`release.ps1`**: Script de PowerShell para releases locales
5. **`.github/workflows/release.yml`**: Workflow de GitHub Actions

### Variables de entorno requeridas:

Para GitHub Actions:
- `GITHUB_TOKEN`: Token de GitHub (automáticamente disponible)
- `DOCKER_USERNAME`: Usuario de Docker Hub
- `DOCKER_PASSWORD`: Contraseña de Docker Hub

## 📝 Uso

### Releases locales

#### Dry Run (Simulación)
```powershell
# Ver qué sería liberado sin hacer cambios
.\release.ps1 -DryRun
```

#### Release real
```powershell
# Realizar release real
.\release.ps1 -Release
```

#### Usando npm directamente
```powershell
# Dry run
npm run release:dry-run

# Release real
npm run release
```

### Releases automáticos

Los releases se ejecutan automáticamente cuando:
1. Se hace push a la rama `master` o `main`
2. Los tests pasan exitosamente
3. Hay commits que ameritan un release según conventional commits

## 📊 Conventional Commits

### Formato de commits

```
<tipo>[scope opcional]: <descripción>

[cuerpo opcional]

[footer(s) opcional(es)]
```

### Tipos de commits y su impacto en versionado:

| Tipo | Descripción | Impacto en versión |
|------|-------------|-------------------|
| `feat` | Nueva funcionalidad | MINOR (1.0.0 → 1.1.0) |
| `fix` | Corrección de bug | PATCH (1.0.0 → 1.0.1) |
| `perf` | Mejora de rendimiento | PATCH |
| `refactor` | Refactorización de código | PATCH |
| `docs` | Cambios en documentación | PATCH |
| `build` | Cambios en build system | PATCH |
| `ci` | Cambios en CI/CD | No genera release |
| `test` | Agregar/modificar tests | No genera release |
| `chore` | Tareas de mantenimiento | No genera release |
| `BREAKING CHANGE` | Cambio que rompe compatibilidad | MAJOR (1.0.0 → 2.0.0) |

### Ejemplos de commits:

```bash
# Nueva funcionalidad (MINOR release)
git commit -m "feat(user-service): add password reset functionality"

# Corrección de bug (PATCH release)
git commit -m "fix(product-service): resolve null pointer exception in search"

# Breaking change (MAJOR release)
git commit -m "feat(api-gateway): redesign authentication API

BREAKING CHANGE: The authentication endpoint has been completely redesigned.
Previous /auth/login endpoint is no longer available."

# No genera release
git commit -m "ci: update Azure DevOps pipeline configuration"
```

## 🔄 Integración con Azure DevOps

### Actualizar pipeline de Azure DevOps

Modifica tu archivo `pipelines/master.yml` para incluir semantic-release:

```yaml
- stage: SemanticRelease
  displayName: 'Semantic Release'
  dependsOn: Tests
  condition: and(succeeded(), eq(variables['Build.SourceBranch'], 'refs/heads/master'))
  jobs:
  - job: Release
    pool:
      vmImage: 'ubuntu-latest'
    steps:
    - checkout: self
      persistCredentials: true
    
    - task: NodeTool@0
      displayName: 'Install Node.js'
      inputs:
        versionSpec: '18.x'
    
    - script: |
        npm ci
        npx semantic-release
      displayName: 'Run Semantic Release'
      env:
        GITHUB_TOKEN: $(GITHUB_TOKEN)
        GIT_AUTHOR_NAME: Azure DevOps
        GIT_AUTHOR_EMAIL: azuredevops@company.com
        GIT_COMMITTER_NAME: Azure DevOps
        GIT_COMMITTER_EMAIL: azuredevops@company.com
```

### Variables requeridas en Azure DevOps

1. Ve a tu proyecto en Azure DevOps
2. Navega a Pipelines → Library
3. Crea o edita tu variable group
4. Agrega:
   - `GITHUB_TOKEN`: Personal Access Token de GitHub

## 📄 Release Notes automáticos

### Ubicación de release notes

Los release notes se generan automáticamente en:
- `CHANGELOG.md`: Changelog acumulativo del proyecto
- `release-notes/`: Carpeta con release notes individuales
- GitHub Releases: Si está configurado GitHub

### Contenido de los release notes

Los release notes incluyen:
- **Versión**: Número de versión semántica
- **Fecha**: Fecha del release
- **Features**: Nuevas funcionalidades
- **Bug Fixes**: Correcciones de bugs
- **Breaking Changes**: Cambios que rompen compatibilidad
- **Performance**: Mejoras de rendimiento
- **Build Information**: Información técnica del build

### Ejemplo de release notes generados

```markdown
# Release Notes v1.2.0

**Release Date:** 13/06/2025

**Version:** 1.2.0

## 🚀 Features
- **user-service**: add password reset functionality
- **product-service**: implement advanced search filters

## 🐛 Bug Fixes
- **order-service**: fix calculation error in total amount
- **api-gateway**: resolve timeout issues with external services

## ⚡ Performance Improvements
- **database**: optimize query performance for product search

## 📦 Build System
- **docker**: update base images to latest versions

## Microservices Included
- 🔧 service-discovery
- ⚙️ cloud-config
- 🌐 api-gateway
- 🔐 proxy-client
- 👤 user-service
- 📦 product-service
- 🛒 order-service
- 💳 payment-service
- ❤️ favourite-service
- 🚚 shipping-service
- 🔍 nginx (reverse proxy)
```

## 🐛 Troubleshooting

### Problemas comunes

#### 1. "No release type found"
**Problema**: No se genera release después del commit
**Solución**: Verifica que uses conventional commits correctamente

```bash
# ❌ Incorrecto
git commit -m "updated user service"

# ✅ Correcto
git commit -m "feat(user-service): add new user registration endpoint"
```

#### 2. "GITHUB_TOKEN not found"
**Problema**: Error de autenticación con GitHub
**Solución**: Configura el token en las variables de entorno

```powershell
# Temporal (para testing local)
$env:GITHUB_TOKEN = "tu_token_aqui"
.\release.ps1 -Release
```

#### 3. "No commits found"
**Problema**: No hay commits desde el último release
**Solución**: Haz al menos un commit con formato conventional

#### 4. "Version update failed in pom.xml"
**Problema**: Error actualizando versiones en archivos Maven
**Solución**: Verifica que el script `scripts/update-version.js` tenga permisos de ejecución

### Logs y debugging

#### Ver logs detallados de semantic-release
```powershell
# Con logs debug
npx semantic-release --debug

# Solo dry-run con logs
npx semantic-release --dry-run --debug
```

#### Verificar configuración
```powershell
# Verificar que la configuración sea válida
npx semantic-release --verify-conditions
```

## 📚 Recursos adicionales

- [Conventional Commits](https://www.conventionalcommits.org/)
- [Semantic Release Documentation](https://semantic-release.gitbook.io/)
- [Semantic Versioning](https://semver.org/)

## 🤝 Contribución

Para contribuir al proyecto:

1. Usa conventional commits
2. Ejecuta `.\release.ps1 -DryRun` antes de hacer push
3. Asegúrate de que los tests pasen
4. Mantén el CHANGELOG.md actualizado automáticamente

---

*Generado automáticamente por el sistema de semantic-release*
