FROM postgres:17.10-bookworm@sha256:4f736ae292687621d4dbe0d499ffd024a36bd2ee7d8ca6f2ccd4c800f047b394

ENV PATH="/opt/patroni/bin:${PATH}" \
    PATRONI_CONFIG_FILE=/etc/patroni/patroni.yml

COPY requirements.txt /tmp/requirements.txt

RUN set -eux; \
    apt-get update; \
    apt-get install -y --no-install-recommends \
        python3=3.11.2-1+b1 \
        python3-venv=3.11.2-1+b1; \
    python3 -m venv /opt/patroni; \
    /opt/patroni/bin/pip install --no-cache-dir -r /tmp/requirements.txt; \
    test "$(/opt/patroni/bin/patroni --version)" = "patroni 4.1.4"; \
    install -d -o postgres -g postgres /etc/patroni; \
    rm -rf /var/lib/apt/lists/* /tmp/requirements.txt

USER postgres

ENTRYPOINT ["patroni"]
CMD ["/etc/patroni/patroni.yml"]
