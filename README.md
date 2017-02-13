# Rx-Gate
Rx operator that functions similar to a filter with time restrictions.  The gate only lets values through 
if they satisfy a threshold function for a certain amount of time. It can also be configured to let values
through after they stop satisfying the threshold function for a specified period of time.

A common use case for a gate is in audio data processing. The gate is usually configured to let audio data through
when the amplitude of the audio signal passes the threshold and stays there for a certain period of time. The audio
is also allowed to pass for a certain amount of time after is has dropped below the threshold amplitude in case the
amplitude picks up again to avoid unnecessary silences.

Example of usage (taken from a audioprocessing rx pipeline)
```
...
AudioReaderObservable.build(sampleRate, blockSize)
    .observeOn(Schedulers.io())
    .subscribeOn(Schedulers.io())
    .lift(
        new Gate<>(
            data -> {
                amplitude = AudioUtils.getAmplitude(data);
                return amplitude > threshold;
            },            
            750,
            250
        )
    ).subscribe(...);
...
```