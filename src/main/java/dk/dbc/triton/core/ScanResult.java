/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.triton.core;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import org.apache.solr.client.solrj.response.TermsResponse;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Solr scan response representation
 * <p>
 * Generates XML on the form:
 * </p>
 * <pre>
 * {@code
 *
 * <ScanResult>
 *   <index>author</index>
 *   <terms>
 *     <term>
 *       <value>a</value>
 *       <frequency>1</frequency>
 *     </term>
 *     <term>
 *       <value>b</value>
 *       <frequency>2</frequency>
 *     </term>
 *     <term>
 *       <value>c</value>
 *       <frequency>3</frequency>
 *     </term>
 *   </terms>
 * </ScanResult>
 * }
 * </pre>
 *
 * <p>
 * Generates JSON on the form:
 * </p>
 * <pre>
 * {@code
 *
 * {
 *   "index" : "author",
 *   "terms" : [ {
 *     "value" : "a",
 *     "frequency" : 1
 *   }, {
 *     "value" : "b",
 *     "frequency" : 2
 *   }, {
 *     "value" : "c",
 *     "frequency" : 3
 *   } ]
 * }
 * }
 * </pre>
 */
public class ScanResult {
    public static final ScanResult EMPTY = new ScanResult(null, Collections.emptyList());

    private String index;
    @JacksonXmlElementWrapper(localName = "terms")
    @JacksonXmlProperty(localName = "term")
    private List<Term> terms;

    public static ScanResult of(TermsResponse termsResponse) {
        final Map<String, List<TermsResponse.Term>> termsMap = termsResponse.getTermMap();
        if (termsMap.size() > 1) {
            throw new IllegalArgumentException("Multiple indexes in TermsResponse");
        }
        final Optional<Map.Entry<String, List<TermsResponse.Term>>> firstEntry =
                termsMap.entrySet().stream().findFirst();
        return firstEntry.map(entry -> new ScanResult(entry.getKey(),
                        entry.getValue().stream()
                                .map(Term::of)
                                .collect(Collectors.toList())))
                .orElse(EMPTY);
    }

    ScanResult(String index, List<Term> terms) {
        this.index = index;
        this.terms = terms;
    }

    public String getIndex() {
        return index;
    }

    public List<Term> getTerms() {
        return terms;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ScanResult that = (ScanResult) o;
        return Objects.equals(index, that.index) &&
                Objects.equals(terms, that.terms);
    }

    @Override
    public int hashCode() {
        return Objects.hash(index, terms);
    }

    public static class Term {
        private final String value;
        private final long frequency;

        public static Term of(TermsResponse.Term solrTerm) {
            return new Term(solrTerm.getTerm(), solrTerm.getFrequency());
        }

        Term(String value, long frequency) {
            this.value = value;
            this.frequency = frequency;
        }

        public String getValue() {
            return value;
        }

        public long getFrequency() {
            return frequency;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Term term = (Term) o;
            return frequency == term.frequency &&
                    Objects.equals(value, term.value);
        }

        @Override
        public int hashCode() {

            return Objects.hash(value, frequency);
        }
    }
}
