package pt.owlsql.extractors.relations;

import java.util.Arrays;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.ToStringRenderer;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLClassExpressionVisitorEx;
import org.semanticweb.owlapi.model.OWLDataAllValuesFrom;
import org.semanticweb.owlapi.model.OWLDataExactCardinality;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataHasValue;
import org.semanticweb.owlapi.model.OWLDataMaxCardinality;
import org.semanticweb.owlapi.model.OWLDataMinCardinality;
import org.semanticweb.owlapi.model.OWLDataSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectComplementOf;
import org.semanticweb.owlapi.model.OWLObjectExactCardinality;
import org.semanticweb.owlapi.model.OWLObjectHasSelf;
import org.semanticweb.owlapi.model.OWLObjectHasValue;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectMaxCardinality;
import org.semanticweb.owlapi.model.OWLObjectMinCardinality;
import org.semanticweb.owlapi.model.OWLObjectOneOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;
import org.semanticweb.owlapi.model.OWLQuantifiedRestriction;
import org.semanticweb.owlapi.util.DefaultPrefixManager;


class ExistentialUnfolder implements OWLClassExpressionVisitorEx<RelationsStore> {
    
    public static void main(String[] args) {
        OWLDataFactory df = OWLManager.getOWLDataFactory();
        DefaultPrefixManager pm = new DefaultPrefixManager("a:");
        
        ToStringRenderer.getInstance().setShortFormProvider(pm);
        
        OWLClass homoSapiens = df.getOWLClass("Homo Sapiens", pm);
        OWLClass organization = df.getOWLClass("Organization", pm);
        OWLClass specimenCollectionObjective = df.getOWLClass("Specimen collection objective", pm);
        OWLClass specimenCollection = df.getOWLClass("Specimen collection", pm);
        OWLClass investigation = df.getOWLClass("Investigation", pm);
        
        OWLObjectProperty inheresIn = df.getOWLObjectProperty("inheres in", pm);
        OWLObjectProperty isBearerOf = df.getOWLObjectProperty("is bearer of", pm);
        OWLObjectProperty concretizes = df.getOWLObjectProperty("concretizes", pm);
        OWLObjectProperty objectiveAchievedBy = df.getOWLObjectProperty("objective achieved by", pm);
        OWLObjectProperty isPartOf = df.getOWLObjectProperty("is part of", pm);
        
        OWLClassExpression x1 = df.getOWLObjectSomeValuesFrom(isPartOf, investigation);
        OWLClassExpression x2 = df.getOWLObjectIntersectionOf(specimenCollection, x1);
        OWLClassExpression x3 = df.getOWLObjectSomeValuesFrom(objectiveAchievedBy, x2);
        OWLClassExpression x4 = df.getOWLObjectIntersectionOf(specimenCollectionObjective, x3);
        OWLClassExpression x5 = df.getOWLObjectSomeValuesFrom(concretizes, x4);
        OWLClassExpression x6 = df.getOWLObjectSomeValuesFrom(isBearerOf, x5);
        OWLClassExpression x7 = df.getOWLObjectUnionOf(homoSapiens, organization);
        OWLClassExpression x8 = df.getOWLObjectIntersectionOf(x6, x7);
        OWLClassExpression x9 = df.getOWLObjectSomeValuesFrom(inheresIn, x8);
        
        RelationsStore store = x9.accept(new ExistentialUnfolder());
        for (Chain chain : store) {
            System.out.println(Arrays.toString(chain.getChain()) + " " + chain.getEndPoint());
        }
        
        /**
         * In here, we test the following concept
         * 
         * <pre>
         * inheresIn some (
         *   (homoSapiens or organization)
         *   and
         *   isBearerOf some (
         *     concretizes some (
         *       specimenCollectionObjective
         *       and
         *       objectiveAchievedBy some (
         *         specimenCollection
         *         and
         *         isPartOf some investigation))))
         * </pre>
         */
    }
    
    
    private RelationsStore directRelations(
            OWLQuantifiedRestriction<OWLClassExpression, OWLObjectPropertyExpression, OWLClassExpression> ce) {
        OWLObjectPropertyExpression property = ce.getProperty();
        if (property.isAnonymous())
            return RelationsStore.empty();
        
        return ce.getFiller().accept(this).prepend(property.asOWLObjectProperty());
    }
    
    
    @Override
    public RelationsStore visit(OWLClass ce) {
        RelationsStore result = new RelationsStore();
        result.add(new Chain(ce));
        return result;
    }
    
    
    @Override
    public RelationsStore visit(OWLDataAllValuesFrom ce) {
        return RelationsStore.empty();
    }
    
    
    @Override
    public RelationsStore visit(OWLDataExactCardinality ce) {
        return RelationsStore.empty();
    }
    
    
    @Override
    public RelationsStore visit(OWLDataHasValue ce) {
        return RelationsStore.empty();
    }
    
    
    @Override
    public RelationsStore visit(OWLDataMaxCardinality ce) {
        return RelationsStore.empty();
    }
    
    
    @Override
    public RelationsStore visit(OWLDataMinCardinality ce) {
        return RelationsStore.empty();
    }
    
    
    @Override
    public RelationsStore visit(OWLDataSomeValuesFrom ce) {
        return RelationsStore.empty();
    }
    
    
    @Override
    public RelationsStore visit(OWLObjectAllValuesFrom ce) {
        return directRelations(ce);
    }
    
    
    @Override
    public RelationsStore visit(OWLObjectComplementOf ce) {
        return RelationsStore.empty();
    }
    
    
    @Override
    public RelationsStore visit(OWLObjectExactCardinality ce) {
        if (ce.getCardinality() == 0)
            return RelationsStore.empty();
        return directRelations(ce);
    }
    
    
    @Override
    public RelationsStore visit(OWLObjectHasSelf ce) {
        return RelationsStore.empty();
    }
    
    
    @Override
    public RelationsStore visit(OWLObjectHasValue ce) {
        return RelationsStore.empty();
    }
    
    
    @Override
    public RelationsStore visit(OWLObjectIntersectionOf ce) {
        RelationsStore result = new RelationsStore();
        for (OWLClassExpression operand : ce.getOperands()) {
            result.addAll(operand.accept(this));
        }
        return result;
    }
    
    
    @Override
    public RelationsStore visit(OWLObjectMaxCardinality ce) {
        if (ce.getCardinality() == 0)
            return RelationsStore.empty();
        return directRelations(ce);
    }
    
    
    @Override
    public RelationsStore visit(OWLObjectMinCardinality ce) {
        if (ce.getCardinality() == 0)
            return RelationsStore.empty();
        return directRelations(ce);
    }
    
    
    @Override
    public RelationsStore visit(OWLObjectOneOf ce) {
        return RelationsStore.empty();
    }
    
    
    @Override
    public RelationsStore visit(OWLObjectSomeValuesFrom ce) {
        return directRelations(ce);
    }
    
    
    @Override
    public RelationsStore visit(OWLObjectUnionOf ce) {
        return RelationsStore.empty();
    }
    
}