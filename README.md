distractor
==========

## project information
Distractor is a continuation of [reactor](https://github.com/FutureProcessing/reactor). The aim of the project is to write
it entirely in [Scala](http://www.scala-lang.org/) with [Akka](http://akka.io/), so that reactors and transports could 
be easily distributed over network instead of being bound to one JVM.

## architecture overview
### actor system
> Note that this architecture overview may not be up to date. Code reference should be the internal actor system indicator
![Actor System Overview][logo]

[logo]: http://yuml.me/diagram/scruffy/class/[note:Distractor ActorSystem {bg:wheat}],[Distractor {bg:lightskyblue}]++-1>[ReactorRegistry {bg:lightskyblue}],[Distractor {bg:lightskyblue}]++-1>[ReactorTransportMixer {bg:lightskyblue}],[Distractor {bg:lightskyblue}]++-1>[TransportRegistry {bg:lightskyblue}],[TransportRegistry {bg:lightskyblue}]<>-0..*>[*Transport {bg:lightsalmon}],[ReactorRegistry {bg:lightskyblue}]<>-0..*>[*Reactor {bg:lightsalmon}]

## issue tracker
[![Stories in Ready](https://badge.waffle.io/gmaslowski/distractor.svg?label=ready&title=Ready)](http://waffle.io/gmaslowski/distractor)
[![Stories in In Progress](https://badge.waffle.io/gmaslowski/distractor.svg?label=in%20progress&title=In%20Progress)](http://waffle.io/gmaslowski/distractor)

[![Throughput Graph](https://graphs.waffle.io/gmaslowski/distractor/throughput.svg)](https://waffle.io/gmaslowski/distractor/metrics)

## continous integration
[![Build Status](https://snap-ci.com/gmaslowski/distractor/branch/develop/build_image)](https://snap-ci.com/gmaslowski/distractor/branch/develop)

