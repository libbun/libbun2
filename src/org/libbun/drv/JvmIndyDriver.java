package org.libbun.drv;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.util.HashMap;
import java.util.Map;

import org.libbun.BunDriver;
import org.libbun.DriverCommand;
import org.libbun.Namespace;
import org.libbun.PegObject;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

public class JvmIndyDriver extends JvmDriver {
	protected final Map<String, Handle> handleMap;

	private final static MethodHandle fbUnaryHande = initFallBackHandle("UnaryOp");
	private final static MethodHandle testUnaryHandle = initTestHandle("UnaryOp", 1);

	private final static MethodHandle fbBinaryHandle = initFallBackHandle("BinaryOp");
	private final static MethodHandle testBinaryHandle = initTestHandle("BinaryOp", 2);

	private final static MethodHandle fbCompHandle = initFallBackHandle("CompOp", Boolean.class);

	private final static MethodHandle fbMethodHandle = initFallBackHandle("Method");
	private final static MethodHandle testMethodHandle = initTestHandleForVarg("Method");

	private final static MethodHandle fbFuncHandle = initFallBackHandle("Func");
	private final static MethodHandle testFuncHandle = initTestHandle("Func", 1);

	public JvmIndyDriver() {
		super("lib/driver/jvm/jvm4python.bun");
		JvmDriver.JAVA_VERSION = V1_7;
		this.addCommand("INDY", new DynamicInvokeCommand());
		this.addCommand("APPLY", new ApplyCommand());

		this.handleMap = new HashMap<String, Handle>();
		this.initBsmHandle("UnaryOp");
		this.initBsmHandle("BinaryOp");
		this.initBsmHandle("CompOp");
		this.initBsmHandle("Method");
		this.initBsmHandle("Func");
	}

	public static class DebuggableJvmIndyDriver extends JvmIndyDriver {
		public DebuggableJvmIndyDriver() {
			JvmByteCodeLoader.setDebugMode(true);
		}
	}

	@Override
	public void initTable(Namespace gamma) {
		super.initTable(gamma);
		this.classMap.put("Integer", Integer.class);
		this.classMap.put("Float", Float.class);
		this.classMap.put("Long", Long.class);
		this.classMap.put("Double", Double.class);
		this.classMap.put("Boolean", Boolean.class);
	}

	protected void initBsmHandle(String name) {
		String bsmName = "bsm" + name;
		Type[] paramTypes = {Type.getType(MethodHandles.Lookup.class), Type.getType(String.class), Type.getType(MethodType.class)};
		Method methodDesc = new Method(bsmName, Type.getType(CallSite.class), paramTypes);
		Handle handle = new Handle(H_INVOKESTATIC, Type.getType(this.getClass()).getInternalName(), bsmName, methodDesc.getDescriptor());
		this.handleMap.put(bsmName, handle);
	}

	protected static MethodHandle initFallBackHandle(String name) {
		return initFallBackHandle(name, Object.class);
	}

	protected static MethodHandle initFallBackHandle(String name, Class<?> returnClass) {
		MethodType type = MethodType.methodType(returnClass, new Class<?>[]{CachedCallSite.class, Object[].class});
		try {
			return MethodHandles.lookup().findStatic(JvmIndyDriver.class, "fallBackFor" + name, type);
		}
		catch(Throwable e) {
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	}

	protected static MethodHandle initTestHandle(String name, int paramSize) {
		Class<?>[] paramClasses = new Class<?>[paramSize * 2];
		for(int i = 0; i < paramSize; i++) {
			paramClasses[i] = Class.class;
			paramClasses[i + paramSize] = Object.class;
		}
		MethodType type = MethodType.methodType(boolean.class, paramClasses);
		try {
			return MethodHandles.lookup().findStatic(JvmIndyDriver.class, "testFor" + name, type);
		}
		catch(Throwable e) {
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	}

	protected static MethodHandle initTestHandleForVarg(String name) {
		Class<?>[] paramClasses = new Class<?>[] {Class[].class, Object[].class};
		MethodType type = MethodType.methodType(boolean.class, paramClasses);
		try {
			return MethodHandles.lookup().findStatic(JvmIndyDriver.class, "testFor" + name, type);
		}
		catch(Throwable e) {
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	}

	/**
	 * INDY [method name] [bootstrap method name] [return class] [param classes...]
	 * @author skgchxngsxyz-osx
	 *
	 */
	protected class DynamicInvokeCommand extends DriverCommand {
		@Override
		public void invoke(BunDriver driver, PegObject node, String[] param) {
			MethodBuilder mBuilder = mBuilders.peek();
			String methodName = param[0];
			int size = param.length;
			int startIndex = 3;
			Type returnType = Type.getType(classMap.get(param[startIndex - 1]));
			Type[] paramTypes = new Type[size - startIndex];
			for(int i = 0; i < paramTypes.length; i++) {
				paramTypes[i] = Type.getType(classMap.get(param[startIndex + i]));
			}
			Type typeDesc = Type.getMethodType(returnType, paramTypes);
			mBuilder.invokeDynamic(methodName, typeDesc.getDescriptor(), handleMap.get(param[1]));
		}
	}

	protected class ApplyCommand extends DriverCommand {
		@Override
		public void invoke(BunDriver driver, PegObject node, String[] param) {
			PegObject targetNode = node.get(0);
			PegObject argsNode = node.get(1);
			if(targetNode.is("#field")) {	// method call
				PegObject recvNode = targetNode.get(0);
				String methodName = targetNode.get(1).getText();
				int paramSize = argsNode.size();
				driver.pushNode(recvNode);
				for(int i = 0 ; i < paramSize; i++) {
					driver.pushNode(argsNode.get(i));
				}
				String typeDesc = this.createDescriptor(paramSize + 1).getDescriptor();
				mBuilders.peek().invokeDynamic(methodName, typeDesc, handleMap.get("bsmMethod"));
			}
			else {	// func call
				driver.pushNode(targetNode);
				int paramSize = argsNode.size();
				for(int i = 0 ; i < paramSize; i++) {
					driver.pushNode(argsNode.get(i));
				}
				String typeDesc = this.createDescriptor(paramSize + 1).getDescriptor();
				mBuilders.peek().invokeDynamic("someythong", typeDesc, handleMap.get("bsmFunc"));
			}
		}

		private Type createDescriptor(int paramSize) {
			Type[] paramTypeDescs = new Type[paramSize];
			Type returnTypeDesc = Type.getType(Object.class);
			for(int i = 0; i < paramSize; i++) {
				paramTypeDescs[i] = Type.getType(Object.class);
			}
			return Type.getMethodType(returnTypeDesc, paramTypeDescs);
		}
	}

	public static class CachedCallSite extends MutableCallSite {
		private final String methodName;
		private final MethodHandles.Lookup lookup;

		private CachedCallSite(String methodName, MethodHandles.Lookup lookup, MethodType type) {
			super(type);
			this.methodName = methodName;
			this.lookup = lookup;
		}
	}

//	private static void reportInvocationError(CachedCallSite callSite, Object[] args, Throwable cause) {
//		StringBuilder sBuilder = new StringBuilder();
//		sBuilder.append("not found method: ");
//		sBuilder.append(callSite.methodName);
//		sBuilder.append('(');
//		for(int i = 0; i < args.length; i++) {
//			if(i > 0) {
//				sBuilder.append(", ");
//			}
//			sBuilder.append(args[i].getClass().getSimpleName());
//		}
//		sBuilder.append('(');
//		sBuilder.append("\n\t");
//		sBuilder.append("caused by -> ");
//		sBuilder.append(cause.getClass().getName());
//		sBuilder.append(": ");
//		sBuilder.append(cause.getMessage());
//		System.err.println(sBuilder.toString());
//	}

	// unary op
	/**
	 * bootstrap method for unary op. used for indy
	 * 
	 * @param lookup
	 * @param methodName
	 * @param type
	 * @return
	 * @throws IllegalAccessException 
	 * @throws NoSuchMethodException 
	 */
	public static CallSite bsmUnaryOp(MethodHandles.Lookup lookup, String methodName, MethodType type) throws Throwable {
		CachedCallSite callSite = new CachedCallSite(methodName, lookup, type);
		MethodHandle fallback = fbUnaryHande.bindTo(callSite);
		fallback = fallback.asCollector(Object[].class, type.parameterCount()).asType(type);
		callSite.setTarget(fallback);
		return callSite;
	}

	/**
	 * fallback handle for unary op. used for indy
	 * @param callSite
	 * @param args
	 * @return
	 * @throws Throwable
	 */
	public static Object fallBackForUnaryOp(CachedCallSite callSite, Object[] args) throws Throwable {
		MethodType type = callSite.type();
		Class<?> rightValueClass = args[0].getClass();
		// target method handle
		MethodHandle targetHandle = 
				callSite.lookup.findStatic(JvmOperator.class, callSite.methodName, 
						type.changeParameterType(0, rightValueClass).changeReturnType(rightValueClass).unwrap());
		targetHandle = targetHandle.asType(type);

		// test method handle
		MethodHandle testHandle = testUnaryHandle.bindTo(rightValueClass);
		testHandle = testHandle.asType(testHandle.type());
		// guard method handle
		MethodHandle guard = MethodHandles.guardWithTest(testHandle, targetHandle, callSite.getTarget());
		callSite.setTarget(guard);
		return targetHandle.invokeWithArguments(args);
	}

	/**
	 * test handle for unary op. used for indy
	 * @param clazz
	 * @param value
	 * @return
	 */
	public static boolean testForUnaryOp(Class<?> clazz, Object value) {
		return value.getClass().equals(clazz);
	}

	//binary op
	/**
	 * boot strap method for binary op, 
	 * @param lookup
	 * @param methodName
	 * @param type
	 * @return
	 * @throws Throwable
	 */
	public static CallSite bsmBinaryOp(MethodHandles.Lookup lookup, String methodName, MethodType type) throws Throwable {
		CachedCallSite callSite = new CachedCallSite(methodName, lookup, type);
		MethodHandle fallback = fbBinaryHandle.bindTo(callSite);
		fallback = fallback.asCollector(Object[].class, type.parameterCount()).asType(type);
		callSite.setTarget(fallback);
		return callSite;
	}

	/**
	 * fallback handle for binary op, used for indy
	 * @param callSite
	 * @param args
	 * @return
	 * @throws Throwable
	 */
	public static Object fallBackForBinaryOp(CachedCallSite callSite, Object[] args) throws Throwable {
		MethodType type = callSite.type();
		Class<?> leftClass = args[0].getClass();
		Class<?> rightClass = args[1].getClass();
		// target handle
		MethodType methodType = type.changeParameterType(0, leftClass).changeParameterType(1, rightClass);
		if(leftClass == String.class || rightClass == String.class) {
			methodType = methodType.changeReturnType(String.class).unwrap();
		}
		else if(leftClass == Double.class || rightClass == Double.class) {
			methodType = methodType.changeReturnType(Double.class).unwrap();
		}
		else {
			methodType = methodType.changeReturnType(methodType.parameterType(0)).unwrap();
		}
		MethodHandle targetHandle = callSite.lookup.findStatic(JvmOperator.class, callSite.methodName, methodType);
		targetHandle = targetHandle.asType(type);
		// test  handle
		MethodHandle testHandle = testBinaryHandle.bindTo(leftClass).bindTo(rightClass);
		testHandle = testHandle.asType(testHandle.type());
		// guard method handle
		MethodHandle guard = MethodHandles.guardWithTest(testHandle, targetHandle, callSite.getTarget());
		callSite.setTarget(guard);
		return targetHandle.invokeWithArguments(args);
	}

	/**
	 * test handle for binary op
	 * @param leftClass
	 * @param rightClass
	 * @param leftValue
	 * @param rightValue
	 * @return
	 */
	public static boolean testForBinaryOp(Class<?> leftClass, Class<?> rightClass, Object leftValue, Object rightValue) {
		return leftValue.getClass().equals(leftClass) && rightValue.getClass().equals(rightClass);
	}

	// compare op
	/**
	 * boot strap method for comparator op. used for indy.
	 * @param lookup
	 * @param methodName
	 * @param type
	 * @return
	 * @throws Throwable
	 */
	public static CallSite bsmCompOp(MethodHandles.Lookup lookup, String methodName, MethodType type) throws Throwable {
		CachedCallSite callSite = new CachedCallSite(methodName, lookup, type);
		MethodHandle fallback = fbCompHandle.bindTo(callSite);
		fallback = fallback.asCollector(Object[].class, type.parameterCount()).asType(type);
		callSite.setTarget(fallback);
		return callSite;
	}

	/**
	 * fallback handle for comparator op. used for indy.
	 * @param callSite
	 * @param args
	 * @return
	 * @throws Throwable
	 */
	public static Boolean fallBackForCompOp(CachedCallSite callSite, Object[] args) throws Throwable {
		MethodType type = callSite.type();
		Class<?> leftClass = args[0].getClass();
		Class<?> rightClass = args[1].getClass();
		// target handle
		MethodType methodType = 
				type.changeParameterType(0, leftClass).
				changeParameterType(1, rightClass).changeReturnType(boolean.class).unwrap();
		MethodHandle targetHandle = callSite.lookup.findStatic(JvmOperator.class, callSite.methodName, methodType);
		targetHandle = targetHandle.asType(type);
		// test handle
		MethodHandle testHandle = testBinaryHandle.bindTo(leftClass).bindTo(rightClass);
		testHandle = testHandle.asType(testHandle.type());
		// guard method handle
		MethodHandle guard = MethodHandles.guardWithTest(testHandle, targetHandle, callSite.getTarget());
		callSite.setTarget(guard);
		return (Boolean) targetHandle.invokeWithArguments(args);
	}

	// method call
	/**
	 * boot strap method for method call. used for indy.
	 * @param lookup
	 * @param methodName
	 * @param type
	 * @return
	 * @throws Throwable
	 */
	public static CallSite bsmMethod(MethodHandles.Lookup lookup, String methodName, MethodType type) throws Throwable {
		CachedCallSite callSite = new CachedCallSite(methodName, lookup, type);
		MethodHandle fallback = fbMethodHandle.bindTo(callSite);
		fallback = fallback.asCollector(Object[].class, type.parameterCount()).asType(type);
		callSite.setTarget(fallback);
		return callSite;
	}

	/**
	 * fallback handle for method call. used for indy.
	 * @param callSite
	 * @param args
	 * @return
	 * @throws Throwable
	 */
	public static Object fallBackForMethod(CachedCallSite callSite, Object[] args) throws Throwable {
		MethodType type = callSite.type();
		int paramSize = args.length;
		Class<?>[] paramClasses = new Class<?>[paramSize];
		for(int i = 0; i < paramSize; i++) {
			paramClasses[i] = args[i].getClass();
		}
		// target handle
		MethodHandle targetHandle = lookupMethodHandle(paramClasses[0], callSite.methodName);
		targetHandle = targetHandle.asType(type);

		// test handle
		MethodHandle testHandle = testMethodHandle.bindTo(paramClasses).asCollector(Object[].class, paramSize);
		// guard handle
		MethodHandle guard = MethodHandles.guardWithTest(testHandle, targetHandle, callSite.getTarget());
		callSite.setTarget(guard);
		return targetHandle.invokeWithArguments(args);
	}

	/**
	 * test handle for method call. used for indy.
	 * @param paramClasses
	 * - include recv class
	 * @param paramas
	 * - include recv object
	 * @return
	 */
	public static boolean testForMethod(Class<?>[] paramClasses, Object[] paramas) {
		int size = paramClasses.length;
		for(int i = 0; i < size; i++) {
			if(!paramClasses[i].equals(paramas[i].getClass())) {
				return false;
			}
		}
		return true;
	}

	private final static Map<Class<?>, vTable> vTableMap = new HashMap<Class<?>, vTable>();

	private static MethodHandle lookupMethodHandle(Class<?> recvClass, String methodName) throws Throwable {
		if(recvClass == null || recvClass.equals(Object.class)) {
			throw new NoSuchMethodException();
		}
		vTable table = vTableMap.get(recvClass);
		if(table == null) {
			table = new vTable(recvClass);
			vTableMap.put(recvClass, table);
		}
		return table.lookup(methodName);
	}

	/**
	 * contains method handle of public instance method.
	 * @author skgchxngsxyz-osx
	 *
	 */
	private static class vTable {
		private final Map<String, MethodHandle> mhMap;

		/**
		 * lookup and set all public methods of recv class.
		 * if found overloaded method, set the last method.
		 * @param recvClass
		 * @throws Throwable 
		 */
		private vTable(Class<?> recvClass) throws Throwable {
			this.mhMap = new HashMap<String, MethodHandle>();
			Lookup lookup = MethodHandles.lookup();
			java.lang.reflect.Method[] methods = recvClass.getMethods();
			for(java.lang.reflect.Method method : methods) {
				MethodHandle handle = lookup.unreflect(method);
				String methodName = method.getName();
				this.mhMap.put(methodName, handle);
			}
		}

		/**
		 * look up method handle.
		 * @param methodName
		 * - method name, do not support method overload.
		 * @return
		 * - if has no method handle, throw exception
		 */
		private MethodHandle lookup(String methodName) throws NoSuchMethodException {
			MethodHandle handle = this.mhMap.get(methodName);
			if(handle == null) {
				throw new NoSuchMethodException(methodName);
			}
			return handle;
		}
	}

	private static MethodHandle lookupFunc(Class<?> funcClass) throws Throwable {
		if(!funcClass.getSuperclass().equals(FuncHolder.class)) {
			throw new RuntimeException("require func class: " + funcClass.getSimpleName());
		}
		MethodHandle handle = funcMap.get(funcClass);
		if(handle != null) {
			return handle;
		}
		java.lang.reflect.Method[] methods = funcClass.getMethods();
		for(java.lang.reflect.Method method : methods) {
			if(method.getName().equals(JvmDriver.funcMethodName)) {
				handle = MethodHandles.lookup().unreflect(method);
				funcMap.put(funcClass, handle);
				return handle;
			}
		}
		throw new NoSuchMethodException();
	}

	private final static Map<Class<?>, MethodHandle> funcMap = new HashMap<Class<?>, MethodHandle>();

	public static CallSite bsmFunc(MethodHandles.Lookup lookup, String methodName, MethodType type) throws Throwable {
		CachedCallSite callSite = new CachedCallSite(methodName, lookup, type);
		MethodHandle fallback = fbFuncHandle.bindTo(callSite);
		fallback = fallback.asCollector(Object[].class, type.parameterCount()).asType(type);
		callSite.setTarget(fallback);
		return callSite;
	}

	public static Object fallBackForFunc(CachedCallSite callSite, Object[] args) throws Throwable {
		MethodType type = callSite.type();
		int paramSize = args.length;
		Class<?>[] paramClasses = new Class<?>[paramSize];
		for(int i = 0; i < paramSize; i++) {
			paramClasses[i] = args[i].getClass();
		}
		// target handle
		MethodHandle targetHandle = lookupFunc(paramClasses[0]);
		targetHandle = targetHandle.asType(type);
		// test handle
		MethodHandle testHandle = testFuncHandle.bindTo(paramClasses[0]);
		// guard handle
		MethodHandle guard = MethodHandles.guardWithTest(testHandle, targetHandle, callSite.getTarget());
		callSite.setTarget(guard);
		return targetHandle.invokeWithArguments(args);
	}

	public static boolean testForFunc(Class<?> recvClass, Object recvObject) {
		return recvObject.getClass().equals(recvClass);
	}
}
