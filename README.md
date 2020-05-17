# dexxt-bitcoin-prototype
Prototype implementation of the DeXTT protocol for Bitcoin, based on the DeXTT Ethereum implementation (https://github.com/pantos-io/dextt-prototype).

The DeXTT protocol is described in detail by the following scientific publication:

> ```
> Michael Borkowski, Marten Sigwart, Philipp Frauenthaler, Taneli Hukkinen, Stefan Schulte, "Dextt: Deterministic Cross-Blockchain Token Transfers," in IEEE Access, vol. 7, pp. 111030-111042, 2019, doi: 10.1109/ACCESS.2019.2934707.
> ```



The program is started via the command line and requires CLI arguments, which can optionally be provided via argumentfiles (examples in `/implementation/argumentfiles`) by specifying a file as `@<file>`.

Inside the `/scripts` folder, bash scripts to start multiple parallel evaluation runs are provided.