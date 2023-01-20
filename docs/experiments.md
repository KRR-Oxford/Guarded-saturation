# 🧪 Experiments

Experiments are available in the `experiments` submodule.

Here we provide a detailed analysis of the results and some instructions for reproducing these experiments.

## [📊 Experiments Results](experiments_results/)

## Reproduce the Experiments

The data from experiments can be large, so they are not included directly in the repository but as an additional submodule.

If you did not clone the repository with the `--recurse-submodules` option, you can initialize the experiments folder using:

```sh
git submodule init
```

For reproducibility purposes, the exact versions of the files used in our experiments are stored in our repository.
In this way, our results can be reproduced even if the original data sources are no longer available.
You can find them [here](https://github.com/mburon/guarded-saturation-experiments/releases/tag/0.0.1).
