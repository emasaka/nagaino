# nagaino

Web API server which expands multi shortened URLs recursively.

## Usage

### Necessities

- leiningen (Clojure environment)
- Maven2

### Environment variables

#### BITLY_USER

API user ID for bit.ly.

#### BITLY_KEY

API key for bit.ly.

#### MONGOHQ_URL (optional)

Specifies MongoDB server.
Format is:

    mongo://<user>:<password>@<host>:<port>/<db>

or:

    mongo://<host>:<port>/<db>

If no value is given, MongoDB is not used.

#### PORT (optional)

TCP port to access.
Default is 8080.

### Run

    $ lein deps
    $ lein run
