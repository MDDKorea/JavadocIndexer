package de.ialistannen.javadocbpi.spoon;

import static de.ialistannen.javadocbpi.model.javadoc.ReferenceConversions.getReference;
import static de.ialistannen.javadocbpi.model.javadoc.ReferenceConversions.renderTypeReference;

import de.ialistannen.javadocbpi.model.elements.DocumentedElement;
import de.ialistannen.javadocbpi.model.elements.DocumentedElementReference;
import de.ialistannen.javadocbpi.model.elements.DocumentedElements;
import de.ialistannen.javadocbpi.model.elements.DocumentedField;
import de.ialistannen.javadocbpi.model.elements.DocumentedMethod;
import de.ialistannen.javadocbpi.model.elements.DocumentedMethod.DocumentedParameter;
import de.ialistannen.javadocbpi.model.elements.DocumentedModule;
import de.ialistannen.javadocbpi.model.elements.DocumentedPackage;
import de.ialistannen.javadocbpi.model.elements.DocumentedType;
import de.ialistannen.javadocbpi.model.elements.DocumentedType.Type;
import de.ialistannen.javadocbpi.model.javadoc.ReferenceConversionVisitor;
import de.ialistannen.javadocbpi.model.javadoc.ReferenceConversions;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;
import spoon.javadoc.api.elements.JavadocElement;
import spoon.javadoc.api.parsing.JavadocParser;
import spoon.reflect.code.CtJavaDoc;
import spoon.reflect.declaration.CtAnnotationMethod;
import spoon.reflect.declaration.CtAnnotationType;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtEnum;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtFormalTypeDeclarer;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtModule;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtRecord;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.CtAbstractVisitor;
import spoon.support.adaption.TypeAdaptor;

/**
 * Oh a javadoc! This references {@link de.ialistannen.javadocbpi.Main}! <br/>
 * <em>Well hello friends :^)</em>
 *
 * @author i_al_istannen
 */
public class Converter extends CtAbstractVisitor {

  private final LongAdder size;
  private final DocumentedElements elements;
  private final AtomicBoolean hasModules;

  public Converter() {
    this.elements = new DocumentedElements();
    this.size = new LongAdder();
    this.hasModules = new AtomicBoolean(false);
  }

  public DocumentedElements getElements() {
    return elements;
  }

  private void addElement(DocumentedElementReference reference, DocumentedElement element) {
    size.increment();
    int value = size.intValue();
    if (value % 1000 == 0) {
      System.out.println("Processed " + value + " elements");
    }
    elements.add(reference, element);
  }

  @Override
  public <A extends Annotation> void visitCtAnnotationType(CtAnnotationType<A> annotationType) {
    visitType(annotationType, Type.ANNOTATION);
  }

  @Override
  public <T> void visitCtClass(CtClass<T> ctClass) {
    visitType(ctClass, Type.CLASS);
  }

  @Override
  public <T extends Enum<?>> void visitCtEnum(CtEnum<T> ctEnum) {
    visitType(ctEnum, Type.ENUM);
  }

  @Override
  public <T> void visitCtField(CtField<T> f) {
    addElement(
        getReference(f),
        new DocumentedField(
            getReference(f.getDeclaringType()),
            f.getSimpleName(),
            getReference(f.getType()),
            renderTypeReference(f.getType()),
            getJavadocComment(f),
            f.getModifiers()
        )
    );
  }

  @Override
  public <T> void visitCtInterface(CtInterface<T> intrface) {
    visitType(intrface, Type.INTERFACE);
  }

  @Override
  public <T> void visitCtConstructor(CtConstructor<T> c) {
    handleExecutable(c, c);
  }

  @Override
  public <T> void visitCtMethod(CtMethod<T> m) {
    handleExecutable(m, m);
  }

  private <T, E extends CtExecutable<T>, D extends CtFormalTypeDeclarer> void handleExecutable(
      E m,
      D d
  ) {
    List<DocumentedParameter> parameters = m.getParameters()
        .stream()
        .map(it -> new DocumentedParameter(
            getReference(it.getType()),
            renderTypeReference(it.getType()),
            it.getSimpleName()
        ))
        .toList();
    List<DocumentedElementReference> thrownTypes = m.getThrownTypes()
        .stream()
        .map(ReferenceConversions::getReference)
        .toList();
    List<String> typeParameters = d.getFormalCtTypeParameters()
        .stream()
        .map(it -> renderTypeReference(it.getReference()))
        .toList();
    addElement(
        getReference(m),
        new DocumentedMethod(
            getReference(d.getDeclaringType()),
            m.getSimpleName(),
            typeParameters,
            getReference(m.getType()),
            renderTypeReference(m.getType()),
            parameters,
            thrownTypes,
            getJavadocComment(m),
            d.getModifiers()
        )
    );
  }

  @Override
  public <T> void visitCtAnnotationMethod(CtAnnotationMethod<T> annotationMethod) {
    visitCtMethod(annotationMethod);
  }

  @Override
  public void visitCtModule(CtModule module) {
    if (!module.isUnnamedModule()) {
      hasModules.set(true);
    }
    addElement(
        getReference(module),
        new DocumentedModule(module.getSimpleName(), getJavadocComment(module))
    );
  }

  @Override
  public void visitCtPackage(CtPackage ctPackage) {
    // Try to ignore fake (?) spoon packages in the unnamed module
    if (hasModules.get() && ctPackage.getDeclaringModule().isUnnamedModule()) {
      return;
    }
    addElement(
        getReference(ctPackage),
        new DocumentedPackage(
            getReference(ctPackage.getDeclaringModule()),
            ctPackage.getSimpleName(),
            getJavadocComment(ctPackage)
        )
    );
  }

  @Override
  public void visitCtRecord(CtRecord recordType) {
    visitType(recordType, Type.RECORD);
  }

  private void visitType(CtType<?> ctType, DocumentedType.Type type) {
    CtTypeReference<Throwable> exceptionReference = ctType.getFactory()
        .Type()
        .<Throwable>get(Throwable.class)
        .getReference();
    List<String> renderedTypeParameters = ctType.getFormalCtTypeParameters()
        .stream()
        .map(it -> renderTypeReference(it.getReference()))
        .toList();

    addElement(
        getReference(ctType),
        new DocumentedType(
            ctType.getSimpleName(),
            getReference(ctType.getPackage()),
            type,
            renderedTypeParameters,
            getJavadocComment(ctType),
            TypeAdaptor.isSubtype(ctType, exceptionReference),
            ctType.getModifiers(),
            ctType.getSuperclass() != null
                ? renderTypeReference(ctType.getSuperclass())
                : "Object",
            ctType.getSuperInterfaces()
                .stream()
                .map(ReferenceConversions::renderTypeReference)
                .toList()
        )
    );
  }

  private static List<JavadocElement> getJavadocComment(CtElement element) {
    List<JavadocElement> elements = element.getComments()
        .stream()
        .filter(comment -> comment instanceof CtJavaDoc)
        .map(comment -> new JavadocParser(comment.getRawContent(), element))
        .map(JavadocParser::parse)
        .flatMap(Collection::stream)
        .toList();

    return new ReferenceConversionVisitor().convertElements(elements);
  }


}
