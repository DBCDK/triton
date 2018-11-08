triton
======

webservice for index scans in dbckat

### configuration

Environment variables:

* ZOOKEEPER mandatory variable pointing to the zookeeper quorum for the Solr cloud instance.
* DEFAULT_COLLECTION optional variable naming the default Solr collection

### API ###

**scan index**

* **URL**

  /scan

* **Method:**

  `GET`

*  **Query params**

   **Required:**
 
   `term`: index term.
   
   `index`: index to scan.
   
   **Optional:**
   
   `collection`: solr collection, defaults to value of environment variable DEFAULT_COLLECTION.
    
    `pos`: preferred term position {first|last}, defaults to first.
     
    `size` maximum number of entries to return, defaults to 20.
     
    `include` restricts to terms matching this regular expression.
    
    `withExactFrequency` perform exact match search for each scan term to adjust term frequencies, defaults to true.
   
    `fieldType` normalize input term before scan using analysis phases defined by this field type, defaults to dbc-phrase.
    
  
* **Success Response:**

  * **Code:** 200 Ok <br />
    **Content:**
    ```json
    {
      "index": "scan.mti",
      "terms": [
        {
          "value": "testing #241 (autbogart)",
          "frequency": 1
        },
        {
          "value": "testing #241 (basis)",
          "frequency": 1
        },
        {
          "value": "testing #241 (bog)",
          "frequency": 1
        },
        {
          "value": "testing #241 (bogart)",
          "frequency": 1
        },
        {
          "value": "testing #241 (bogpla)",
          "frequency": 1
        }
      ]  
    }
    ```

* **Sample Call:**

  ```bash
  curl -vs 'https://tritonhost/triton/scan?index=mti&term=testing'
  ```

### development

**Requirements**

To build this project JDK 1.8 and Apache Maven is required.

To start a local instance, docker is required.

**Scripts**
* clean - clears build artifacts
* build - builds artifacts
* test - runs unit and integration tests
* validate - analyzes source code and javadoc
* start - starts localhost instance
* stop - stops localhost instance

```bash
./clean && ./build && ./test && ./validate && ZOOKEEPER="..." DEFAULT_COLLECTION="..." ./start
```

```bash
curl -vs 'http://localhost:8080/triton/scan?size=10&index=mti&term=testing'
```
### License

Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3.
See license text in LICENSE.txt