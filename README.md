# AML_IDE

IDE Android para desenvolvimento em **C/C++** direto no celular, com toolchain
**Clang/LLVM** embutido e sistema de recursos remoto.

![License](https://img.shields.io/badge/license-MIT-blue.svg)
![Platform](https://img.shields.io/badge/platform-Android-green.svg)
![Language](https://img.shields.io/badge/language-Java-orange.svg)
![Git LFS](https://img.shields.io/badge/Git-LFS-blueviolet.svg)

---

## ✨ Recursos

- 📝 **Editor com abas** e destaque de sintaxe configurável via JSON
  (C, C++, Java, XML, JSON, YAML, Shell, PowerShell, Markdown, Gradle, Properties)
- 🖥️ **Console/Terminal integrado** para ver a saída da compilação
- 🧰 **Compilador Clang/LLVM nativo** rodando dentro do app (aarch64)
- 🔗 **Clonagem de repositórios** do GitHub direto pelo app
- 📁 **Explorador de arquivos** em árvore
- ⚙️ **Gerenciador de projetos** e dependências
- 📦 **Sistema de recursos remoto**: baixa toolchain e sysroot sob demanda,
  reduzindo o tamanho do APK
- 🎨 **Tema configurável** e modo imersivo

---

## 🏗️ Arquitetura

O projeto segue uma adaptação de **MVC**:

```

app/src/main/java/
├── app/
│   ├── controladores/       → orquestração de fluxos
│   │   ├── ControladorApp
│   │   ├── ControladorDialogos
│   │   ├── ControladorMontadores
│   │   ├── ControladorNavegacao
│   │   └── ControladorPaineis
│   ├── modelo/              → regras de negócio e persistência
│   │   ├── Projeto
│   │   ├── Dependencia
│   │   ├── ManifestoRecursos
│   │   ├── RecursoRemoto
│   │   └── Gerenciador*
│   ├── ui/                  → interface (views)
│   │   ├── componentes/     widgets reutilizáveis
│   │   ├── editor/sintaxe/  highlight baseado em JSON
│   │   ├── explorador/      árvore de arquivos
│   │   ├── telas/           telas completas
│   │   ├── dialogos/        diálogos modais
│   │   ├── navegacao/       roteamento entre telas
│   │   ├── estilos/         tema centralizado
│   │   └── util/            helpers de UI
│   └── util/                → utilitários gerais
│       ├── CompiladorNativo
│       ├── GerenciadorNDK
│       ├── GerenciadorRecursos
│       ├── DownloadHelper
│       ├── Preferencias
│       └── compilacao/      → pipeline de compilação
│           ├── AbiUtils
│           ├── AmbienteCompilacao
│           ├── ConstrutorComando
│           └── ExecutorClang
└── globalclass/             → configurações globais e utilitários

```

### 🧠 Pipeline de compilação

A compilação é dividida em etapas bem definidas:

1. **`AmbienteCompilacao`** — configura variáveis de ambiente (`PATH`,
   `LD_LIBRARY_PATH`, sysroot) para o Clang funcionar
2. **`AbiUtils`** — identifica a arquitetura alvo (`arm64-v8a`, `armeabi-v7a`)
3. **`ConstrutorComando`** — monta o comando `clang ...` com todas as flags
   corretas (`-shared`, `-fPIC`, `-I`, `-L`, etc.)
4. **`ExecutorClang`** — executa o compilador e captura a saída

---

## 🚀 Como clonar

> ⚠️ **Este repositório usa Git LFS** para armazenar as bibliotecas nativas
> (`.so`, `.a`, `.o`) do Clang/LLVM. Sem o LFS, os binários vêm apenas como
> ponteiros de texto e o app **não compila**.

```bash
# 1. Instale o Git LFS (uma vez só)
git lfs install

# 2. Clone normalmente
git clone https://github.com/MAIKOTS/AML-IDE-Android.git
cd AML-IDE-Android
```

Se você já clonou antes de instalar o LFS:

```bash
git lfs pull
```

---

🔨 Como compilar

Pré-requisitos

· Android Studio (versão recente)
· Android SDK (API 21+)
· Android NDK (para o toolchain nativo)
· Git LFS (para baixar as bibliotecas)

Passos

```bash
# Linux / macOS
./gradlew assembleDebug

# Windows
gradlew.bat assembleDebug
```

O APK será gerado em:

```
app/build/outputs/apk/debug/app-debug.apk
```

Para instalar direto no dispositivo conectado:

```bash
./gradlew installDebug
```

---

📦 Tamanho do APK

O app embarca o toolchain Clang/LLVM (~180 MB descompactado) mas baixa
sysroot e headers de um repositório remoto na primeira execução,
mantendo o APK final enxuto.

Métrica Valor
APK compactado ~66 MB
Conteúdo descompactado ~184 MB
Recursos baixados na 1ª execução ~100 MB

---

📂 Estrutura de jniLibs

As bibliotecas nativas que vão dentro do APK:

```
app/src/main/jniLibs/arm64-v8a/
├── libc++_shared.so        Runtime C++
├── libclang.so             Compilador Clang/LLVM (96 MB)
├── libldreal.so            Linker real (lld)
├── libldwrapper.so         Wrapper do linker
├── libllvm-ar.so           Criador de bibliotecas .a
├── libllvm-objcopy.so      Manipulador de objetos
├── libllvm-ranlib.so       Indexador de .a
└── libllvm-strip.so        Strip de binários
```

---

🤝 Contribuindo

1. Faça um fork do projeto
2. Crie uma branch (git checkout -b feature/minha-feature)
3. Commit suas mudanças (git commit -m "Adiciona X")
4. Push para a branch (git push origin feature/minha-feature)
5. Abra um Pull Request

Lembre-se de ter o Git LFS instalado antes de clonar o fork.

---

📜 Licença

Este projeto está sob a licença MIT. Veja o arquivo LICENSE
para mais detalhes.

---

👤 Autor

MAIKOTS

· GitHub: @MAIKOTS
· Repositório: github.com/MAIKOTS/AML-IDE-Android

---
