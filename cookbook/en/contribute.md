# How to Contribute

Thank you for your interest in the AgentScope Runtime Java project.

AgentScope Runtime Java is an open-source project focused on agent deployment and secure tool execution, with a friendly developer community that is happy to help new contributors. We welcome all types of contributions, from code improvements to documentation writing.

## Community

- Join our DingTalk group:

  | DingTalk                                                     |
  | ------------------------------------------------------------ |
  | <img src="https://img.alicdn.com/imgextra/i1/O1CN01LxzZha1thpIN2cc2E_!!6000000005934-2-tps-497-477.png" width="100" height="100"> |

## Reporting Issues

### Bugs

If you find a bug in AgentScope Runtime Java, please first test with the latest version to ensure your issue hasn't been fixed. If not, please search our issue list on GitHub to see if a similar issue has already been raised.

- If you confirm the bug hasn't been reported, please submit a bug issue before writing any code. When submitting an issue, please include:
- Clear problem description
- Steps to reproduce
- Code/error messages
- Environment details (operating system, Java dependency details)
- Affected components (e.g., Engine module, Sandbox module, or both)

### Security Issues

If you find a security issue in AgentScope Runtime Java, please report it to us through the [Alibaba Security Response Center (ASRC)](https://security.alibaba.com/).

## Feature Requests

If you would like AgentScope Runtime Java to have a feature that doesn't exist, please submit a feature request issue on GitHub, describing:

- The feature and its purpose
- How it should work
- Security considerations (if applicable)

## Improving Documentation

Please refer to {doc}`README`

## Contributing Code

If you want to contribute new features or bug fixes to AgentScope Runtime, please first discuss your ideas in a GitHub issue. If no related issue exists, please create one. Someone might already be working on it, or it might have special complexities (especially security considerations for Sandbox features) that you should be aware of before starting to code.

### Fork and Create a Branch

Fork the AgentScope Runtime Java main branch code and clone it to your local machine. For help, see the GitHub help pages.

Create a branch with a descriptive name.

```bash
git checkout -b feature/your-feature-name
```

### Making Changes

- Write clear, well-commented code
- Follow existing code style
- Add tests for new features/fixes
- Update documentation as needed

### Test Your Changes

Run the test suite to ensure your changes don't break existing functionality:

```bash
mvn test
```

### Submitting Your Changes

1. Commit your changes with a clear message:

```bash
git commit -m "Add: brief description of your changes"
```

2. Push to your Fork:

```bash
git push origin feature/your-feature-name
```

3. Create a Pull Request (PR) from your branch to the main repository with a **clear description**

### Code Review Process

- All PRs require review by maintainers
- Address any feedback or requested changes
- Once approved, your PR will be merged

Thank you for contributing to AgentScope Runtime Java!



