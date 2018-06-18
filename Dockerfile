FROM java:8-jre-alpine
USER nobody
EXPOSE 4567/tcp
ENV SOURCE_JAR_DIR=/app/lib
CMD ["java","-cp","/app/lib/*","io.mfj.textricator.web.Main","/pdfs"]
COPY target/docker/lib/* /app/lib/
COPY target/docker/jar/* /app/lib/
