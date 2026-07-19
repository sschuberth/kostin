# Introduction

Kostin is a simple command line tool to communicate with the REST-based API V2 for PIKO IQ and PLENTICORE plus inverters.
Currently, only the PLENTICORE G3 inverter is tested.

# Quickstart

To get started with Kostin, follow these steps:

1. Checkout the source code.
2. Run `./gradlew -q :cli:runJvm --args="--help"`:

```
Usage: main [<options>] <command> [<args>]...

Options:
* --url=<text>
  -h, --help    Show this message and exit

Commands:
  version
  log
  reboot
```

# Acknowledgments

* https://github.com/stegm/pykoplenti (Python, `Apache-2.0`)
* https://github.com/kilianknoll/kostal-RESTAPI (Python, `GPL-3.0-or-later`)

# Disclaimer

This project is not affiliated with, sponsored by, or endorsed by [Kostal](https://www.kostal-solar-electric.com/) and / or any mentioned company or trademark owner.
