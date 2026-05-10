FROM ubuntu:latest
LABEL authors="wojte"

ENTRYPOINT ["top", "-b"]