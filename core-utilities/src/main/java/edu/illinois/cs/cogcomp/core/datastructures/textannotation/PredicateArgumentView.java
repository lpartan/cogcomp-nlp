package edu.illinois.cs.cogcomp.core.datastructures.textannotation;

import edu.illinois.cs.cogcomp.core.algorithms.Sorters;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.CoNLLColumnFormatReader;

import java.util.*;

/**
 * @author Vivek Srikumar
 */
public class PredicateArgumentView extends View {

    private static final long serialVersionUID = 4100738005147066812L;
    private List<Constituent> predicates;

    /**
     * Create a new PredicateArgumentView with default {@link #viewGenerator} and {@link #score}.
     *
     * @param viewName the name of the view
     * @param text the TextAnnotation to augment
     */
    public PredicateArgumentView(String viewName, TextAnnotation text) {
        this(viewName, viewName + "-annotator", text, 1.0);
    }

    public PredicateArgumentView(String viewName, String viewGenerator, TextAnnotation text,
            double score) {
        super(viewName, viewGenerator, text, score);
        predicates = new ArrayList<>();
    }

    public void addPredicateArguments(Constituent predicate, List<Constituent> args,
            String[] relations, double[] scores) {
        if (args.size() != relations.length) {
            throw new IllegalArgumentException("Number of arguments != number of relations");
        }

        if (args.size() != scores.length) {
            throw new IllegalArgumentException("Number of arguments != number of scores");
        }

        this.addConstituent(predicate);
        this.predicates.add(predicate);

        for (int i = 0; i < args.size(); i++) {
            this.addConstituent(args.get(i));
            this.addRelation(new Relation(relations[i], predicate, args.get(i), scores[i]));
        }
    }

    private void findPredicates() {
        // The hypothesis is that all nodes with no incoming edges are predicates.
        for (Constituent c : this.getConstituents()) {
            if (c.getIncomingRelations().size() == 0)
                this.predicates.add(c);
        }
    }

    public List<Relation> getArguments(Constituent predicate) {
        if (!predicates.contains(predicate)) {
            throw new IllegalArgumentException("Predicate " + predicate + " not found");
        }

        return predicate.getOutgoingRelations();
    }

    public List<Constituent> getPredicates() {
        if (this.predicates.size() == 0) {
            findPredicates();
        }

        return this.predicates;
    }

    public String getPredicateLemma(Constituent predicate) {
        if (predicate.hasAttribute(CoNLLColumnFormatReader.LemmaIdentifier))
            return predicate.getAttribute(CoNLLColumnFormatReader.LemmaIdentifier);
        else
            return predicate.getTokenizedSurfaceForm().toLowerCase().trim();

    }

    public String getPredicateSense(Constituent predicate) {
        if (predicate.hasAttribute(CoNLLColumnFormatReader.SenseIdentifer))
            return predicate.getAttribute(CoNLLColumnFormatReader.SenseIdentifer);
        else
            return "";
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        List<Constituent> p = new ArrayList<>(this.getPredicates());

        Collections.sort(p, TextAnnotationUtilities.constituentStartComparator);

        for (Constituent predicate : p) {
            String pred = getPredicateLemma(predicate) + ":" + getPredicateSense(predicate);

            sb.append(pred);
            sb.append("\n");

            StringBuilder spaces = new StringBuilder();
            for (int i = 0; i < 4; i++)
                spaces.append(" ");

            List<Relation> outgoingRelations = new ArrayList<>(predicate.getOutgoingRelations());

            Collections.sort(outgoingRelations, new Comparator<Relation>() {

                @Override
                public int compare(Relation arg0, Relation arg1) {
                    return arg0.getRelationName().compareTo(arg1.getRelationName());
                }
            });

            for (Relation r : outgoingRelations) {
                Constituent target = r.getTarget();
                sb.append(spaces).append(r.getRelationName()).append(": ")
                        .append(target.getTokenizedSurfaceForm());

                if (target.getAttributeKeys().size() > 0) {
                    sb.append("[");
                    for (String key : Sorters.sortSet(target.getAttributeKeys())) {
                        sb.append(key).append("=").append(target.getAttribute(key)).append(" ");
                    }
                    sb.append("]");
                }
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    public Set<Constituent> getPredicateArgumentConstituents() {
        Set<Constituent> allCons = new HashSet<>(this.predicates);
        for(Constituent c : allCons) {
            for(Relation r : c.getOutgoingRelations() ) {
                allCons.add(r.getTarget());
            }
        }
        return allCons;
    }

    public Set<Relation> getPredicateArgumentRelations() {
        Set<Relation> relations = new HashSet<>();
        for(Constituent c: getPredicateArgumentConstituents()) {
            for(Relation r: c.getIncomingRelations())
                relations.add(r);
            for(Relation r: c.getOutgoingRelations())
                relations.add(r);
        }
        return relations;
    }

    public void removeAllConstituentsAndRelations() {
        Set<Relation> allRelations = getPredicateArgumentRelations();
        for(Constituent c : getPredicateArgumentConstituents()) {
            this.removeConstituent(c);
        }
        for(Relation r : allRelations) {
            this.removeRelation(r);
        }
    }

    public void removeAllRelations() {
        for(Constituent c: getPredicateArgumentConstituents()) {
            // remove relations from the View class
            for(Relation r: c.getIncomingRelations())
                this.removeRelation(r);
            for(Relation r: c.getOutgoingRelations())
                this.removeRelation(r);
            // remove relations from the constituents
            c.removeAllIncomingRelatons();
            c.removeAllOutgoingRelaton();
        }
    }
}
