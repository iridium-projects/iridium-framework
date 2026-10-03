package cc.asylum.iridium.config.processor.walk;

import cc.asylum.forgery.expr.Expr;
import cc.asylum.iridium.codegen.Processing;
import cc.asylum.iridium.config.ConfigurationProperties;
import cc.asylum.iridium.config.Default;
import cc.asylum.iridium.config.Value;
import cc.asylum.iridium.core.validation.annotation.Nullable;
import org.junit.jupiter.api.Test;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.WildcardType;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class ConfigWalkTest {

  @Test
  void rejectsInvalidRootsAndBindsScalars() {
    final Fixture fixture = new Fixture();
    final ConfigWalk walk = new ConfigWalk(fixture.processing);
    assertSame(walk, walk);
    assertNotNull(walk.deeper());

    final TypeElement abstractType = type("app.AbstractConfig", ElementKind.CLASS, "app");
    when(abstractType.getModifiers()).thenAnswer(invocation -> Set.of(Modifier.ABSTRACT, Modifier.PUBLIC));
    assertNull(walk.bindRoot(abstractType));

    final TypeElement generic = type("app.GenericConfig", ElementKind.CLASS, "app");
    when(generic.getTypeParameters()).thenAnswer(invocation -> List.<TypeParameterElement>of(mock(TypeParameterElement.class)));
    assertNull(walk.bindRoot(generic));

    final TypeElement badPrefix = type("app.BadPrefix", ElementKind.CLASS, ".bad");
    assertNull(new ConfigWalk(fixture.processing).bindRoot(badPrefix));

    final TypeElement noCtor = type("app.NoCtor", ElementKind.CLASS, "app");
    when(noCtor.getEnclosedElements()).thenAnswer(invocation -> List.<Element>of());
    assertNull(new ConfigWalk(fixture.processing).bindRoot(noCtor));

    final TypeElement hidden = type("app.Hidden", ElementKind.CLASS, "app");
    final ExecutableElement hiddenCtor = constructor(Modifier.PRIVATE);
    when(hidden.getEnclosedElements()).thenAnswer(invocation -> List.<Element>of(hiddenCtor));
    assertNull(new ConfigWalk(fixture.processing).bindRoot(hidden));

    final TypeElement empty = type("app.Empty", ElementKind.CLASS, "");
    final ExecutableElement emptyCtor = constructor(Modifier.PUBLIC);
    when(empty.getEnclosedElements()).thenAnswer(invocation -> List.<Element>of(emptyCtor));
    assertNotNull(new ConfigWalk(fixture.processing).bindRoot(empty));

    final TypeElement named = type("app.Named", ElementKind.CLASS, "app");
    when(named.getEnclosedElements()).thenAnswer(invocation -> List.<Element>of(constructor(Modifier.PUBLIC)));
    assertNotNull(new ConfigWalk(fixture.processing).bindRoot(named));

    final VariableElement missing = parameter("name", kind(TypeKind.DECLARED), null);
    assertNull(new ConfigWalk(fixture.processing).bindValue(missing));

    final VariableElement badValue = parameter("name", primitive(TypeKind.INT), value("plain"));
    assertNull(new ConfigWalk(fixture.processing).bindValue(badValue));

    final VariableElement objectValue = parameter("widget", fixture.declared("app.Widget", ElementKind.CLASS), value("${widget}"));
    assertNull(new ConfigWalk(fixture.processing).bindValue(objectValue));

    final VariableElement intValue = parameter("count", primitive(TypeKind.INT), value("${app.count}"));
    assertNotNull(new ConfigWalk(fixture.processing).bindValue(intValue));

    final VariableElement stringValue = parameter("name", fixture.same(String.class), value("${app.name:fallback}"));
    assertNotNull(new ConfigWalk(fixture.processing).bindValue(stringValue));

    final VariableElement nullable = parameter("name", fixture.same(String.class), value("${app.name}"));
    when(nullable.getAnnotation(Nullable.class)).thenAnswer(invocation -> mock(Nullable.class));
    assertNotNull(new ConfigWalk(fixture.processing).bindValue(nullable));

    final VariableElement primitiveNull = parameter("flag", primitive(TypeKind.BOOLEAN), value("${app.flag}"));
    when(primitiveNull.getAnnotation(Nullable.class)).thenAnswer(invocation -> mock(Nullable.class));
    assertNull(new ConfigWalk(fixture.processing).bindValue(primitiveNull));
  }

  @Test
  void bindsClassesRecordsCollectionsMapsAndNestedTypes() {
    final Fixture fixture = new Fixture();

    final TypeElement scalarClass = type("app.Scalars", ElementKind.CLASS, "app");
    final VariableElement count = parameter("count", primitive(TypeKind.INT), null);
    final VariableElement flag = parameter("flag", primitive(TypeKind.BOOLEAN), null);
    final Default fallback = mock(Default.class);
    when(fallback.value()).thenAnswer(invocation -> "true");
    when(flag.getAnnotation(Default.class)).thenAnswer(invocation -> fallback);
    final VariableElement unnamed = parameter("arg0", primitive(TypeKind.LONG), null);
    final ExecutableElement scalarCtor = constructor(Modifier.PUBLIC, count, flag, unnamed);
    when(scalarClass.getEnclosedElements()).thenAnswer(invocation -> List.<Element>of(scalarCtor));
    assertNull(new ConfigWalk(fixture.processing).bindRoot(scalarClass));

    final TypeElement okClass = type("app.Ok", ElementKind.CLASS, "ok");
    final VariableElement title = parameter("title", fixture.same(String.class), null);
    when(okClass.getEnclosedElements()).thenAnswer(invocation -> List.<Element>of(constructor(Modifier.PUBLIC, title)));
    assertNotNull(new ConfigWalk(fixture.processing).bindRoot(okClass));

    final TypeElement record = type("app.Settings", ElementKind.RECORD, "settings");
    final RecordComponentElement host = component("host");
    final RecordComponentElement port = component("port");
    final VariableElement hostParam = parameter("ignored", fixture.same(String.class), null);
    final VariableElement portParam = parameter("ignored", primitive(TypeKind.INT), null);
    final Value valued = value("${server.host:localhost}");
    when(host.getAnnotation(Value.class)).thenAnswer(invocation -> valued);
    when(record.getRecordComponents()).thenAnswer(invocation -> List.of(host, port));
    final ExecutableElement recordCtor = constructor(Modifier.PUBLIC, hostParam, portParam);
    when(record.getEnclosedElements()).thenAnswer(invocation -> List.<Element>of(recordCtor, constructor(Modifier.PUBLIC)));
    assertNotNull(new ConfigWalk(fixture.processing).bindRoot(record));

    final TypeElement badValueRecord = type("app.BadValue", ElementKind.RECORD, "bad");
    final RecordComponentElement raw = component("raw");
    when(raw.getAnnotation(Value.class)).thenAnswer(invocation -> value("not-a-placeholder"));
    final VariableElement rawParam = parameter("raw", fixture.same(String.class), null);
    when(badValueRecord.getRecordComponents()).thenAnswer(invocation -> List.of(raw));
    when(badValueRecord.getEnclosedElements()).thenAnswer(invocation -> List.<Element>of(constructor(Modifier.PUBLIC, rawParam)));
    assertNull(new ConfigWalk(fixture.processing).bindRoot(badValueRecord));

    final TypeElement listType = type("app.Tags", ElementKind.CLASS, "tags");
    final VariableElement tags = parameter(
        "items",
        fixture.collection("java.util.List", fixture.same(String.class)),
        null);
    when(listType.getEnclosedElements()).thenAnswer(invocation -> List.<Element>of(constructor(Modifier.PUBLIC, tags)));
    assertNotNull(new ConfigWalk(fixture.processing).bindRoot(listType));

    final TypeElement setType = type("app.Ids", ElementKind.CLASS, "ids");
    final VariableElement ids = parameter("ids", fixture.collection("java.util.Set", primitive(TypeKind.INT)), null);
    when(setType.getEnclosedElements()).thenAnswer(invocation -> List.<Element>of(constructor(Modifier.PUBLIC, ids)));
    assertNotNull(new ConfigWalk(fixture.processing).bindRoot(setType));

    final TypeElement mapType = type("app.Table", ElementKind.CLASS, "table");
    final VariableElement table = parameter("table", fixture.map(fixture.same(String.class), primitive(TypeKind.LONG)), null);
    when(mapType.getEnclosedElements()).thenAnswer(invocation -> List.<Element>of(constructor(Modifier.PUBLIC, table)));
    assertNotNull(new ConfigWalk(fixture.processing).bindRoot(mapType));

    final TypeElement nested = type("app.Nested", ElementKind.CLASS, "nested");
    final TypeElement child = type("app.Child", ElementKind.CLASS, "child");
    when(child.getEnclosedElements()).thenAnswer(invocation -> List.<Element>of(
        constructor(Modifier.PUBLIC, parameter("name", fixture.same(String.class), null))));
    final VariableElement childParam = parameter("child", fixture.mirror(child), null);
    when(nested.getEnclosedElements()).thenAnswer(invocation -> List.<Element>of(constructor(Modifier.PUBLIC, childParam)));
    assertNotNull(new ConfigWalk(fixture.processing).bindRoot(nested));

    final TypeElement optionalType = type("app.Maybe", ElementKind.CLASS, "maybe");
    final VariableElement maybe = parameter("maybe", fixture.optional(fixture.same(String.class)), null);
    when(optionalType.getEnclosedElements()).thenAnswer(invocation -> List.<Element>of(constructor(Modifier.PUBLIC, maybe)));
    assertNotNull(new ConfigWalk(fixture.processing).bindRoot(optionalType));

    final TypeElement nullableNested = type("app.NullableNested", ElementKind.CLASS, "outer");
    final VariableElement nullableChild = parameter("child", fixture.mirror(child), null);
    when(nullableChild.getAnnotation(Nullable.class)).thenAnswer(invocation -> mock(Nullable.class));
    when(nullableNested.getEnclosedElements()).thenAnswer(invocation -> List.<Element>of(constructor(Modifier.PUBLIC, nullableChild)));
    assertNotNull(new ConfigWalk(fixture.processing).bindRoot(nullableNested));

    final TypeElement listOfObjects = type("app.Nodes", ElementKind.CLASS, "nodes");
    final VariableElement nodes = parameter("nodes", fixture.collection("java.util.List", fixture.mirror(child)), null);
    when(listOfObjects.getEnclosedElements()).thenAnswer(invocation -> List.<Element>of(constructor(Modifier.PUBLIC, nodes)));
    assertNotNull(new ConfigWalk(fixture.processing).bindRoot(listOfObjects));
  }

  @Test
  void reportsUnsupportedShapes() {
    final Fixture fixture = new Fixture();

    final TypeElement cycle = type("app.Cycle", ElementKind.CLASS, "cycle");
    final VariableElement self = parameter("self", fixture.mirror(cycle), null);
    when(cycle.getEnclosedElements()).thenAnswer(invocation -> List.<Element>of(constructor(Modifier.PUBLIC, self)));
    assertNull(new ConfigWalk(fixture.processing).bindRoot(cycle));

    final TypeElement primitiveNull = type("app.Prim", ElementKind.CLASS, "prim");
    final VariableElement flag = parameter("flag", primitive(TypeKind.BOOLEAN), null);
    when(flag.getAnnotation(Nullable.class)).thenAnswer(invocation -> mock(Nullable.class));
    when(primitiveNull.getEnclosedElements()).thenAnswer(invocation -> List.<Element>of(constructor(Modifier.PUBLIC, flag)));
    assertNull(new ConfigWalk(fixture.processing).bindRoot(primitiveNull));

    final TypeElement arrayType = type("app.Arrays", ElementKind.CLASS, "arrays");
    final VariableElement array = parameter("values", kind(TypeKind.ARRAY), null);
    when(arrayType.getEnclosedElements()).thenAnswer(invocation -> List.<Element>of(constructor(Modifier.PUBLIC, array)));
    assertNull(new ConfigWalk(fixture.processing).bindRoot(arrayType));

    final TypeElement errorType = type("app.Errors", ElementKind.CLASS, "errors");
    final TypeMirror error = kind(TypeKind.ERROR);
    when(error.toString()).thenAnswer(invocation -> "missing.Type");
    when(fixture.types.asElement(error)).thenAnswer(invocation -> null);
    final VariableElement broken = parameter("broken", error, null);
    when(errorType.getEnclosedElements()).thenAnswer(invocation -> List.<Element>of(constructor(Modifier.PUBLIC, broken)));
    assertNull(new ConfigWalk(fixture.processing).bindRoot(errorType));

    final TypeElement declaredError = type("app.DeclaredError", ElementKind.CLASS, "declared");
    final DeclaredType notType = mock(DeclaredType.class);
    when(notType.getKind()).thenAnswer(invocation -> TypeKind.DECLARED);
    when(notType.toString()).thenAnswer(invocation -> "not.A.Type");
    when(fixture.types.asElement(notType)).thenAnswer(invocation -> mock(Element.class));
    final VariableElement mystery = parameter("mystery", notType, null);
    when(declaredError.getEnclosedElements()).thenAnswer(invocation -> List.<Element>of(constructor(Modifier.PUBLIC, mystery)));
    assertNull(new ConfigWalk(fixture.processing).bindRoot(declaredError));

    final TypeElement defaultObject = type("app.DefaultObject", ElementKind.CLASS, "obj");
    final TypeElement child = type("app.Child2", ElementKind.CLASS, "child");
    when(child.getEnclosedElements()).thenAnswer(invocation -> List.<Element>of(constructor(Modifier.PUBLIC)));
    final VariableElement nested = parameter("child", fixture.mirror(child), null);
    final Default fallback = mock(Default.class);
    when(fallback.value()).thenAnswer(invocation -> "nope");
    when(nested.getAnnotation(Default.class)).thenAnswer(invocation -> fallback);
    when(defaultObject.getEnclosedElements()).thenAnswer(invocation -> List.<Element>of(constructor(Modifier.PUBLIC, nested)));
    assertNull(new ConfigWalk(fixture.processing).bindRoot(defaultObject));

    final TypeElement rawList = type("app.RawList", ElementKind.CLASS, "raw");
    final VariableElement raw = parameter("items", fixture.collection("java.util.List"), null);
    when(rawList.getEnclosedElements()).thenAnswer(invocation -> List.<Element>of(constructor(Modifier.PUBLIC, raw)));
    assertNull(new ConfigWalk(fixture.processing).bindRoot(rawList));

    final TypeElement wild = type("app.Wild", ElementKind.CLASS, "wild");
    final WildcardType wildcard = mock(WildcardType.class);
    when(wildcard.getKind()).thenAnswer(invocation -> TypeKind.WILDCARD);
    final VariableElement wildParam = parameter("items", fixture.collection("java.util.List", wildcard), null);
    when(wild.getEnclosedElements()).thenAnswer(invocation -> List.<Element>of(constructor(Modifier.PUBLIC, wildParam)));
    assertNull(new ConfigWalk(fixture.processing).bindRoot(wild));

    final TypeElement defaultList = type("app.DefaultList", ElementKind.CLASS, "defs");
    final VariableElement listed = parameter("items", fixture.collection("java.util.List", fixture.same(String.class)), null);
    when(listed.getAnnotation(Default.class)).thenAnswer(invocation -> fallback);
    when(defaultList.getEnclosedElements()).thenAnswer(invocation -> List.<Element>of(constructor(Modifier.PUBLIC, listed)));
    assertNull(new ConfigWalk(fixture.processing).bindRoot(defaultList));

    final TypeElement enumItems = type("app.EnumItems", ElementKind.CLASS, "modes");
    final TypeMirror enumeration = fixture.declared("pkg.Mode", ElementKind.ENUM);
    final VariableElement modes = parameter("modes", fixture.collection("java.util.Set", enumeration), null);
    when(enumItems.getEnclosedElements()).thenAnswer(invocation -> List.<Element>of(constructor(Modifier.PUBLIC, modes)));
    assertNotNull(new ConfigWalk(fixture.processing).bindRoot(enumItems));

    final TypeElement badItem = type("app.BadItem", ElementKind.CLASS, "bad-items");
    final TypeMirror mysteryItem = kind(TypeKind.ERROR);
    when(mysteryItem.toString()).thenAnswer(invocation -> "missing.Item");
    when(fixture.types.asElement(mysteryItem)).thenAnswer(invocation -> null);
    final VariableElement items = parameter("items", fixture.collection("java.util.List", mysteryItem), null);
    when(badItem.getEnclosedElements()).thenAnswer(invocation -> List.<Element>of(constructor(Modifier.PUBLIC, items)));
    assertNull(new ConfigWalk(fixture.processing).bindRoot(badItem));

    final TypeElement rawMap = type("app.RawMap", ElementKind.CLASS, "raw-map");
    final VariableElement mapped = parameter("table", fixture.map(), null);
    when(rawMap.getEnclosedElements()).thenAnswer(invocation -> List.<Element>of(constructor(Modifier.PUBLIC, mapped)));
    assertNull(new ConfigWalk(fixture.processing).bindRoot(rawMap));

    final TypeElement intKey = type("app.IntKey", ElementKind.CLASS, "int-key");
    final VariableElement keyed = parameter("table", fixture.map(primitive(TypeKind.INT), fixture.same(String.class)), null);
    when(intKey.getEnclosedElements()).thenAnswer(invocation -> List.<Element>of(constructor(Modifier.PUBLIC, keyed)));
    assertNull(new ConfigWalk(fixture.processing).bindRoot(intKey));

    final TypeElement objectValue = type("app.ObjectMap", ElementKind.CLASS, "object-map");
    final TypeElement valueType = type("app.Value", ElementKind.CLASS, "value");
    when(valueType.getEnclosedElements()).thenAnswer(invocation -> List.<Element>of(constructor(Modifier.PUBLIC)));
    final VariableElement values = parameter("table", fixture.map(fixture.same(String.class), fixture.mirror(valueType)), null);
    when(objectValue.getEnclosedElements()).thenAnswer(invocation -> List.<Element>of(constructor(Modifier.PUBLIC, values)));
    assertNull(new ConfigWalk(fixture.processing).bindRoot(objectValue));

    final TypeElement defaultMap = type("app.DefaultMap", ElementKind.CLASS, "default-map");
    final VariableElement withDefault = parameter("table", fixture.map(fixture.same(String.class), primitive(TypeKind.INT)), null);
    when(withDefault.getAnnotation(Default.class)).thenAnswer(invocation -> fallback);
    when(defaultMap.getEnclosedElements()).thenAnswer(invocation -> List.<Element>of(constructor(Modifier.PUBLIC, withDefault)));
    assertNull(new ConfigWalk(fixture.processing).bindRoot(defaultMap));

    final TypeElement optionalFail = type("app.OptionalFail", ElementKind.CLASS, "optional-fail");
    final VariableElement optionalArray = parameter("maybe", fixture.optional(kind(TypeKind.ARRAY)), null);
    when(optionalFail.getEnclosedElements()).thenAnswer(invocation -> List.<Element>of(constructor(Modifier.PUBLIC, optionalArray)));
    assertNull(new ConfigWalk(fixture.processing).bindRoot(optionalFail));

    final TypeElement badNested = type("app.BadNested", ElementKind.CLASS, "bad-nested");
    final TypeElement abstractChild = type("app.AbstractChild", ElementKind.CLASS, "child");
    when(abstractChild.getModifiers()).thenAnswer(invocation -> Set.of(Modifier.PUBLIC));
    when(abstractChild.getEnclosedElements()).thenAnswer(invocation -> List.<Element>of());
    final VariableElement childParam = parameter("child", fixture.mirror(abstractChild), null);
    when(badNested.getEnclosedElements()).thenAnswer(invocation -> List.<Element>of(constructor(Modifier.PUBLIC, childParam)));
    assertNull(new ConfigWalk(fixture.processing).bindRoot(badNested));

    final TypeElement nullableFail = type("app.NullableFail", ElementKind.CLASS, "nullable-fail");
    final VariableElement nullableChild = parameter("child", fixture.mirror(abstractChild), null);
    when(nullableChild.getAnnotation(Nullable.class)).thenAnswer(invocation -> mock(Nullable.class));
    when(nullableFail.getEnclosedElements()).thenAnswer(invocation -> List.<Element>of(constructor(Modifier.PUBLIC, nullableChild)));
    assertNull(new ConfigWalk(fixture.processing).bindRoot(nullableFail));

    final TypeElement multi = type("app.Multi", ElementKind.CLASS, "multi");
    final VariableElement left = parameter("left", primitive(TypeKind.INT), null);
    final VariableElement right = parameter("right", fixture.same(String.class), null);
    when(multi.getEnclosedElements()).thenAnswer(invocation -> List.<Element>of(constructor(Modifier.PUBLIC, left, right)));
    assertNotNull(new ConfigWalk(fixture.processing).bindRoot(multi));

    final TypeElement boxed = type("app.Boxed", ElementKind.CLASS, "boxed");
    final VariableElement number = parameter("number", fixture.same(Integer.class), null);
    final VariableElement bool = parameter("bool", fixture.same(Boolean.class), null);
    final VariableElement wide = parameter("wide", fixture.same(Long.class), null);
    final VariableElement ratio = parameter("ratio", fixture.same(Double.class), null);
    final VariableElement small = parameter("small", fixture.same(Float.class), null);
    final VariableElement shorter = parameter("shorter", fixture.same(Short.class), null);
    final VariableElement tiny = parameter("tiny", fixture.same(Byte.class), null);
    when(boxed.getEnclosedElements()).thenAnswer(invocation -> List.<Element>of(
        constructor(Modifier.PUBLIC, number, bool, wide, ratio, small, shorter, tiny)));
    assertNotNull(new ConfigWalk(fixture.processing).bindRoot(boxed));

    final TypeElement enums = type("app.Enums", ElementKind.CLASS, "enums");
    final TypeElement mode = enumType("pkg.Mode");
    final VariableElement modeParam = parameter("mode", fixture.mirror(mode), null);
    when(enums.getEnclosedElements()).thenAnswer(invocation -> List.<Element>of(constructor(Modifier.PUBLIC, modeParam)));
    assertNotNull(new ConfigWalk(fixture.processing).bindRoot(enums));

    final TypeElement nullableEnum = type("app.NullableEnum", ElementKind.CLASS, "nullable-enum");
    final VariableElement nullableMode = parameter("mode", fixture.mirror(mode), null);
    when(nullableMode.getAnnotation(Nullable.class)).thenAnswer(invocation -> mock(Nullable.class));
    when(nullableEnum.getEnclosedElements()).thenAnswer(invocation -> List.<Element>of(constructor(Modifier.PUBLIC, nullableMode)));
    assertNotNull(new ConfigWalk(fixture.processing).bindRoot(nullableEnum));

    final TypeElement enumDefault = type("app.EnumDefault", ElementKind.CLASS, "enum-default");
    final VariableElement defaultMode = parameter("mode", fixture.mirror(mode), null);
    final Default modeDefault = mock(Default.class);
    when(modeDefault.value()).thenAnswer(invocation -> "DEV");
    when(defaultMode.getAnnotation(Default.class)).thenAnswer(invocation -> modeDefault);
    when(enumDefault.getEnclosedElements()).thenAnswer(invocation -> List.<Element>of(constructor(Modifier.PUBLIC, defaultMode)));
    assertNotNull(new ConfigWalk(fixture.processing).bindRoot(enumDefault));

    final TypeElement badEnum = type("app.BadEnum", ElementKind.CLASS, "bad-enum");
    final VariableElement missingMode = parameter("mode", fixture.mirror(mode), null);
    final Default missing = mock(Default.class);
    when(missing.value()).thenAnswer(invocation -> "NOPE");
    when(missingMode.getAnnotation(Default.class)).thenAnswer(invocation -> missing);
    when(badEnum.getEnclosedElements()).thenAnswer(invocation -> List.<Element>of(constructor(Modifier.PUBLIC, missingMode)));
    assertNull(new ConfigWalk(fixture.processing).bindRoot(badEnum));

    final TypeElement primitives = type("app.Primitives", ElementKind.CLASS, "prims");
    final VariableElement a = parameter("a", primitive(TypeKind.DOUBLE), null);
    final VariableElement b = parameter("b", primitive(TypeKind.FLOAT), null);
    final VariableElement c = parameter("c", primitive(TypeKind.SHORT), null);
    final VariableElement d = parameter("d", primitive(TypeKind.BYTE), null);
    final VariableElement e = parameter("e", primitive(TypeKind.LONG), null);
    when(primitives.getEnclosedElements()).thenAnswer(invocation -> List.<Element>of(constructor(Modifier.PUBLIC, a, b, c, d, e)));
    assertNotNull(new ConfigWalk(fixture.processing).bindRoot(primitives));

    final TypeElement nullableInt = type("app.NullableInt", ElementKind.CLASS, "nullable-int");
    final VariableElement nullableNumber = parameter("count", fixture.same(Integer.class), null);
    when(nullableNumber.getAnnotation(Nullable.class)).thenAnswer(invocation -> mock(Nullable.class));
    when(nullableInt.getEnclosedElements()).thenAnswer(invocation -> List.<Element>of(constructor(Modifier.PUBLIC, nullableNumber)));
    assertNotNull(new ConfigWalk(fixture.processing).bindRoot(nullableInt));

    final TypeElement badDefault = type("app.BadDefault", ElementKind.CLASS, "bad-default");
    final VariableElement bad = parameter("count", primitive(TypeKind.INT), null);
    final Default badValue = mock(Default.class);
    when(badValue.value()).thenAnswer(invocation -> "nope");
    when(bad.getAnnotation(Default.class)).thenAnswer(invocation -> badValue);
    when(badDefault.getEnclosedElements()).thenAnswer(invocation -> List.<Element>of(constructor(Modifier.PUBLIC, bad)));
    assertNull(new ConfigWalk(fixture.processing).bindRoot(badDefault));

    final TypeElement componentDefault = type("app.ComponentDefault", ElementKind.RECORD, "component");
    final RecordComponentElement piece = component("name");
    final Default componentFallback = mock(Default.class);
    when(componentFallback.value()).thenAnswer(invocation -> "text");
    when(piece.getAnnotation(Default.class)).thenAnswer(invocation -> componentFallback);
    final VariableElement pieceParam = parameter("name", fixture.same(String.class), null);
    when(componentDefault.getRecordComponents()).thenAnswer(invocation -> List.of(piece));
    when(componentDefault.getEnclosedElements()).thenAnswer(invocation -> List.<Element>of(constructor(Modifier.PUBLIC, pieceParam)));
    assertNotNull(new ConfigWalk(fixture.processing).bindRoot(componentDefault));

    final TypeElement blankValue = type("app.BlankValue", ElementKind.CLASS, "blank");
    final VariableElement blank = parameter("name", fixture.same(String.class), value("${ }"));
    when(blankValue.getEnclosedElements()).thenAnswer(invocation -> List.<Element>of(constructor(Modifier.PUBLIC, blank)));
    assertNull(new ConfigWalk(fixture.processing).bindRoot(blankValue));

    final TypeElement valueFallback = type("app.ValueFallback", ElementKind.CLASS, "value-fallback");
    final VariableElement valued = parameter("count", primitive(TypeKind.INT), value("${app.count}"));
    final Default valuedDefault = mock(Default.class);
    when(valuedDefault.value()).thenAnswer(invocation -> "4");
    when(valued.getAnnotation(Default.class)).thenAnswer(invocation -> valuedDefault);
    when(valueFallback.getEnclosedElements()).thenAnswer(invocation -> List.<Element>of(constructor(Modifier.PUBLIC, valued)));
    assertNotNull(new ConfigWalk(fixture.processing).bindRoot(valueFallback));

    final TypeElement optionalObject = type("app.OptionalObject", ElementKind.CLASS, "optional-object");
    final TypeElement optionalChild = type("app.OptionalChild", ElementKind.CLASS, "child");
    when(optionalChild.getEnclosedElements()).thenAnswer(invocation -> List.<Element>of(constructor(Modifier.PUBLIC)));
    final VariableElement optionalNested = parameter("child", fixture.optional(primitive(TypeKind.INT)), null);
    when(optionalObject.getEnclosedElements()).thenAnswer(invocation -> List.<Element>of(constructor(Modifier.PUBLIC, optionalNested)));
    assertNotNull(new ConfigWalk(fixture.processing).bindRoot(optionalObject));

    final TypeElement brokenItems = type("app.BrokenItems", ElementKind.CLASS, "broken-items");
    final TypeElement brokenChild = type("app.BrokenChild", ElementKind.CLASS, "broken");
    when(brokenChild.getEnclosedElements()).thenAnswer(invocation -> List.<Element>of());
    final VariableElement brokenNodes = parameter(
        "nodes",
        fixture.collection("java.util.List", fixture.mirror(brokenChild)),
        null);
    when(brokenItems.getEnclosedElements()).thenAnswer(invocation -> List.<Element>of(constructor(Modifier.PUBLIC, brokenNodes)));
    assertNull(new ConfigWalk(fixture.processing).bindRoot(brokenItems));

    final TypeElement charType = type("app.Chars", ElementKind.CLASS, "chars");
    final VariableElement letter = parameter("letter", primitive(TypeKind.CHAR), null);
    when(charType.getEnclosedElements()).thenAnswer(invocation -> List.<Element>of(constructor(Modifier.PUBLIC, letter)));
    assertNull(new ConfigWalk(fixture.processing).bindRoot(charType));
  }

  private static TypeElement type(final String qualified, final ElementKind kind, final String prefix) {
    final TypeElement type = mock(TypeElement.class);
    final Name name = mock(Name.class);
    when(name.toString()).thenAnswer(invocation -> qualified);
    when(type.getQualifiedName()).thenAnswer(invocation -> name);
    when(type.getKind()).thenAnswer(invocation -> kind);
    when(type.getModifiers()).thenAnswer(invocation -> Set.of(Modifier.PUBLIC));
    when(type.getTypeParameters()).thenAnswer(invocation -> List.<TypeParameterElement>of());
    when(type.getEnclosedElements()).thenAnswer(invocation -> List.<Element>of());
    when(type.getRecordComponents()).thenAnswer(invocation -> List.<RecordComponentElement>of());
    final ConfigurationProperties properties = mock(ConfigurationProperties.class);
    when(properties.value()).thenAnswer(invocation -> prefix);
    when(type.getAnnotation(ConfigurationProperties.class)).thenAnswer(invocation -> properties);
    return type;
  }

  private static TypeElement enumType(final String qualified) {
    final TypeElement type = type(qualified, ElementKind.ENUM, "");
    final VariableElement constant = mock(VariableElement.class);
    when(constant.getKind()).thenAnswer(invocation -> ElementKind.ENUM_CONSTANT);
    final Name simple = mock(Name.class);
    when(simple.contentEquals("DEV")).thenAnswer(invocation -> true);
    when(simple.contentEquals(org.mockito.ArgumentMatchers.anyString())).thenAnswer(invocation -> "DEV".equals(invocation.getArgument(0)));
    when(constant.getSimpleName()).thenAnswer(invocation -> simple);
    when(type.getEnclosedElements()).thenAnswer(invocation -> List.<Element>of(constant));
    return type;
  }

  private static ExecutableElement constructor(final Modifier modifier, final VariableElement... parameters) {
    final ExecutableElement constructor = mock(ExecutableElement.class);
    when(constructor.getKind()).thenAnswer(invocation -> ElementKind.CONSTRUCTOR);
    when(constructor.getModifiers()).thenAnswer(invocation -> Set.of(modifier));
    when(constructor.getParameters()).thenAnswer(invocation -> List.of(parameters));
    return constructor;
  }

  private static VariableElement parameter(final String simple, final TypeMirror type, final Value value) {
    final VariableElement parameter = mock(VariableElement.class);
    final Name name = mock(Name.class);
    when(name.toString()).thenAnswer(invocation -> simple);
    when(parameter.getSimpleName()).thenAnswer(invocation -> name);
    when(parameter.asType()).thenAnswer(invocation -> type);
    when(parameter.getKind()).thenAnswer(invocation -> ElementKind.PARAMETER);
    when(parameter.getAnnotation(Value.class)).thenAnswer(invocation -> value);
    return parameter;
  }

  private static RecordComponentElement component(final String simple) {
    final RecordComponentElement component = mock(RecordComponentElement.class);
    final Name name = mock(Name.class);
    when(name.toString()).thenAnswer(invocation -> simple);
    when(component.getSimpleName()).thenAnswer(invocation -> name);
    when(component.getKind()).thenAnswer(invocation -> ElementKind.RECORD_COMPONENT);
    return component;
  }

  private static Value value(final String raw) {
    final Value value = mock(Value.class);
    when(value.value()).thenAnswer(invocation -> raw);
    return value;
  }

  private static TypeMirror kind(final TypeKind kind) {
    final TypeMirror type = mock(TypeMirror.class);
    when(type.getKind()).thenAnswer(invocation -> kind);
    when(type.toString()).thenAnswer(invocation -> kind.name());
    return type;
  }

  private static TypeMirror primitive(final TypeKind kind) {
    return kind(kind);
  }

  private static final class Fixture {

    private final javax.lang.model.util.Types types = mock(javax.lang.model.util.Types.class);
    private final javax.lang.model.util.Elements elements = mock(javax.lang.model.util.Elements.class);
    private final Processing processing = new Processing(
        types,
        elements,
        mock(Messager.class),
        mock(javax.annotation.processing.Filer.class),
        mock(javax.annotation.processing.RoundEnvironment.class));

    private Fixture() {
      final java.util.Map<String, TypeMirror> mirrors = new java.util.LinkedHashMap<>();
      for (final Class<?> target : List.of(
          String.class,
          Boolean.class,
          Integer.class,
          Long.class,
          Double.class,
          Float.class,
          Short.class,
          Byte.class)) {
        final TypeElement element = mock(TypeElement.class);
        final Name name = mock(Name.class);
        when(name.toString()).thenAnswer(invocation -> target.getCanonicalName());
        when(element.getQualifiedName()).thenAnswer(invocation -> name);
        when(element.getKind()).thenAnswer(invocation -> ElementKind.CLASS);
        final TypeMirror mirror = mock(TypeMirror.class);
        when(element.asType()).thenAnswer(invocation -> mirror);
        when(elements.getTypeElement(target.getCanonicalName())).thenAnswer(invocation -> element);
        mirrors.put(target.getCanonicalName(), mirror);
      }
      when(types.isSameType(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
          .thenAnswer(invocation -> {
            final Object left = invocation.getArgument(0);
            final Object right = invocation.getArgument(1);
            if (!(left instanceof TypeMirror type) || type.getKind() != TypeKind.DECLARED) {
              return false;
            }
            final Element owner = types.asElement(type);
            if (!(owner instanceof TypeElement typeElement)) {
              return false;
            }
            return right == mirrors.get(typeElement.getQualifiedName().toString());
          });
    }

    private TypeMirror same(final Class<?> target) {
      final DeclaredType type = mock(DeclaredType.class);
      when(type.getKind()).thenAnswer(invocation -> TypeKind.DECLARED);
      final TypeElement element = (TypeElement) elements.getTypeElement(target.getCanonicalName());
      when(type.asElement()).thenAnswer(invocation -> element);
      when(types.asElement(type)).thenAnswer(invocation -> element);
      when(type.getTypeArguments()).thenAnswer(invocation -> List.<TypeMirror>of());
      when(type.toString()).thenAnswer(invocation -> target.getCanonicalName());
      return type;
    }

    private TypeMirror declared(final String qualified, final ElementKind kind) {
      final DeclaredType type = mock(DeclaredType.class);
      when(type.getKind()).thenAnswer(invocation -> TypeKind.DECLARED);
      final TypeElement element = mock(TypeElement.class);
      final Name name = mock(Name.class);
      when(name.toString()).thenAnswer(invocation -> qualified);
      when(element.getQualifiedName()).thenAnswer(invocation -> name);
      when(element.getKind()).thenAnswer(invocation -> kind);
      when(type.asElement()).thenAnswer(invocation -> element);
      when(types.asElement(type)).thenAnswer(invocation -> element);
      when(type.getTypeArguments()).thenAnswer(invocation -> List.<TypeMirror>of());
      when(type.toString()).thenAnswer(invocation -> qualified);
      return type;
    }

    private TypeMirror mirror(final TypeElement element) {
      final DeclaredType type = mock(DeclaredType.class);
      when(type.getKind()).thenAnswer(invocation -> TypeKind.DECLARED);
      when(type.asElement()).thenAnswer(invocation -> element);
      when(types.asElement(type)).thenAnswer(invocation -> element);
      when(type.getTypeArguments()).thenAnswer(invocation -> List.<TypeMirror>of());
      when(type.toString()).thenAnswer(invocation -> element.getQualifiedName().toString());
      return type;
    }

    private DeclaredType collection(final String qualified, final TypeMirror... arguments) {
      final DeclaredType type = mock(DeclaredType.class);
      when(type.getKind()).thenAnswer(invocation -> TypeKind.DECLARED);
      final TypeElement element = mock(TypeElement.class);
      final Name name = mock(Name.class);
      when(name.toString()).thenAnswer(invocation -> qualified);
      when(element.getQualifiedName()).thenAnswer(invocation -> name);
      when(element.getKind()).thenAnswer(invocation -> ElementKind.INTERFACE);
      when(type.asElement()).thenAnswer(invocation -> element);
      when(types.asElement(type)).thenAnswer(invocation -> element);
      when(type.getTypeArguments()).thenAnswer(invocation -> List.of(arguments));
      when(type.toString()).thenAnswer(invocation -> qualified);
      return type;
    }

    private DeclaredType map(final TypeMirror... arguments) {
      return collection("java.util.Map", arguments);
    }

    private DeclaredType optional(final TypeMirror inner) {
      final DeclaredType type = collection("java.util.Optional", inner);
      return type;
    }
  }
}
